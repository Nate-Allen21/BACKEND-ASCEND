package br.com.ascend.the_system.controller;

import br.com.ascend.the_system.domain.ProfilePost;
import br.com.ascend.the_system.domain.PostLike;
import br.com.ascend.the_system.domain.PostComment;
import br.com.ascend.the_system.domain.SocialConnection;
import br.com.ascend.the_system.domain.User;
import br.com.ascend.the_system.dto.ProfilePostRequest;
import br.com.ascend.the_system.dto.ProfileUpdateRequest;
import br.com.ascend.the_system.exception.BadRequestException;
import br.com.ascend.the_system.exception.UnauthorizedException;
import br.com.ascend.the_system.repository.ProfilePostRepository;
import br.com.ascend.the_system.repository.PostLikeRepository;
import br.com.ascend.the_system.repository.PostCommentRepository;
import br.com.ascend.the_system.repository.SocialConnectionRepository;
import br.com.ascend.the_system.repository.UserRepository;
import br.com.ascend.the_system.repository.EquippedCosmeticRepository;
import br.com.ascend.the_system.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private static final int MAX_PROFILE_IMAGE_BYTES = 96 * 1024;
    private static final String PROFILE_IMAGE_PREFIX = "data:image/jpeg;base64,";
    private static final int MAX_PROFILE_IMAGE_LENGTH = PROFILE_IMAGE_PREFIX.length() + ((MAX_PROFILE_IMAGE_BYTES + 2) / 3) * 4;
    private final AuthService auth;
    private final UserRepository users;
    private final ProfilePostRepository posts;
    private final SocialConnectionRepository connections;
    private final PostLikeRepository likes;
    private final PostCommentRepository comments;
    private final EquippedCosmeticRepository equippedCosmetics;
    public ProfileController(AuthService auth, UserRepository users, ProfilePostRepository posts, SocialConnectionRepository connections, PostLikeRepository likes, PostCommentRepository comments, EquippedCosmeticRepository equippedCosmetics) { this.auth=auth; this.users=users; this.posts=posts; this.connections=connections; this.likes=likes; this.comments=comments; this.equippedCosmetics=equippedCosmetics; }

    @GetMapping
    @Transactional(readOnly = true)
    public Map<String,Object> mine(@RequestHeader(value="Authorization",required=false) String header) {
        User u=auth.resolveUser(header);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("id",u.getId()); result.put("name",u.getName()); result.put("username",u.getUsername()); result.put("avatar",u.getAvatar()); result.put("profileImage",u.getProfileImage());
        result.put("bio",u.getBio()); result.put("tags",splitTags(u.getProfileTags())); result.put("visibility",u.getProfileVisibility());
        result.put("joinedAt",u.getCreatedAt()); result.put("level",u.getLevel()); result.put("rank",u.getRank());
        result.put("xp",u.getExperience()); result.put("nextLevelXp",u.getNextLevelExperience()); result.put("streakDays",u.getStreakDays());
        result.put("followers",connections.findByTargetAndKindAndStatus(u,"FOLLOW","ACCEPTED").size());
        result.put("following",connections.findBySourceAndKindAndStatus(u,"FOLLOW","ACCEPTED").size());
        result.put("posts",posts.findTop50ByUserOrderByCreatedAtDesc(u).stream().map(p->postMap(p,u)).toList());
        result.put("equippedCosmetics",equippedCosmetics.findByUser_Id(u.getId()).stream().map(this::equippedCosmeticMap).toList());
        return result;
    }

    @GetMapping("/discover") @Transactional(readOnly=true)
    public List<Map<String,Object>> discover(@RequestHeader(value="Authorization",required=false) String header,@RequestParam(defaultValue="") String q){
        User me=auth.resolveUser(header); if(q.trim().length()<2) return List.of();
        return users.findTop20ByNameContainingIgnoreCaseOrderByNameAsc(q.trim()).stream().filter(u->!u.getId().equals(me.getId())&&!blocked(me,u)).map(u->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",u.getId());m.put("name",u.getName());m.put("username",u.getUsername());m.put("avatar",u.getAvatar());m.put("following",connections.existsBySourceAndTargetAndKindAndStatus(me,u,"FOLLOW","ACCEPTED"));m.put("friendStatus",friendStatus(me,u));return m;}).toList();
    }

    @GetMapping("/players/{id}") @Transactional(readOnly=true)
    public Map<String,Object> player(@RequestHeader(value="Authorization",required=false) String header,@PathVariable Long id){
        User me=auth.resolveUser(header), target=users.findById(id).orElseThrow(()->new BadRequestException("Jogador não encontrado."));
        boolean self=me.getId().equals(id), friends=areFriends(me,target);
        if(!self && (blocked(me,target) || blocked(target,me))) throw new BadRequestException("Este perfil não está disponível.");
        String visibility=target.getProfileVisibility();
        if(!self && (visibility.equals("PRIVATE") || (visibility.equals("FRIENDS")&&!friends))) throw new BadRequestException("Este perfil é visível somente para amigos.");
        Map<String,Object> result=new LinkedHashMap<>(); result.put("id",target.getId());result.put("name",target.getName());result.put("username",target.getUsername());result.put("avatar",target.getAvatar());result.put("profileImage",target.getProfileImage());result.put("bio",target.getBio());result.put("tags",splitTags(target.getProfileTags()));result.put("level",target.getLevel());result.put("rank",target.getRank());result.put("joinedAt",target.getCreatedAt());
        result.put("posts",posts.findTop50ByUserOrderByCreatedAtDesc(target).stream().map(p->postMap(p,me)).toList());result.put("equippedCosmetics",equippedCosmetics.findByUser_Id(target.getId()).stream().map(this::equippedCosmeticMap).toList());result.put("following",connections.existsBySourceAndTargetAndKindAndStatus(me,target,"FOLLOW","ACCEPTED"));result.put("friendStatus",friendStatus(me,target));return result;
    }

    @PostMapping("/follow/{id}") @Transactional
    public void follow(@RequestHeader(value="Authorization",required=false) String header,@PathVariable Long id){User me=auth.resolveUser(header), target=other(id,me); if(connections.existsBySourceAndTargetAndKind(me,target,"FOLLOW"))return; connections.save(new SocialConnection(me,target,"FOLLOW","ACCEPTED"));}
    @DeleteMapping("/follow/{id}") @Transactional
    public void unfollow(@RequestHeader(value="Authorization",required=false) String header,@PathVariable Long id){User me=auth.resolveUser(header), target=other(id,me); connections.findBySourceAndTargetAndKind(me,target,"FOLLOW").ifPresent(connections::delete);}
    @PostMapping("/friends/{id}") @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> requestFriend(@RequestHeader(value="Authorization",required=false) String header,@PathVariable Long id){User me=auth.resolveUser(header),target=other(id,me); if(areFriends(me,target))return Map.of("status","ACCEPTED");
        Optional<SocialConnection> reciprocal=connections.findBySourceAndTargetAndKind(target,me,"FRIEND"); if(reciprocal.isPresent()&&reciprocal.get().getStatus().equals("PENDING")){reciprocal.get().setStatus("ACCEPTED");connections.save(reciprocal.get());return Map.of("status","ACCEPTED");}
        if(!connections.existsBySourceAndTargetAndKind(me,target,"FRIEND")) connections.save(new SocialConnection(me,target,"FRIEND","PENDING"));return Map.of("status","PENDING");}
    @PutMapping("/friends/{id}") @Transactional
    public Map<String,String> acceptFriend(@RequestHeader(value="Authorization",required=false) String header,@PathVariable Long id){User me=auth.resolveUser(header),target=other(id,me);SocialConnection request=connections.findBySourceAndTargetAndKind(target,me,"FRIEND").filter(c->c.getStatus().equals("PENDING")).orElseThrow(()->new BadRequestException("Solicitação pendente não encontrada."));request.setStatus("ACCEPTED");connections.save(request);return Map.of("status","ACCEPTED");}
    @DeleteMapping("/friends/{id}") @Transactional
    public void removeFriend(@RequestHeader(value="Authorization",required=false) String header,@PathVariable Long id){User me=auth.resolveUser(header),target=other(id,me);connections.findBySourceAndTargetAndKind(me,target,"FRIEND").ifPresent(connections::delete);connections.findBySourceAndTargetAndKind(target,me,"FRIEND").ifPresent(connections::delete);}
    @GetMapping("/friends/requests") @Transactional(readOnly=true)
    public List<Map<String,Object>> friendRequests(@RequestHeader(value="Authorization",required=false) String header){User me=auth.resolveUser(header);return connections.findByTargetAndKindAndStatus(me,"FRIEND","PENDING").stream().map(c->Map.<String,Object>of("id",c.getSource().getId(),"name",c.getSource().getName(),"username",c.getSource().getUsername())).toList();}
    @GetMapping("/followers") @Transactional(readOnly=true)
    public List<Map<String,Object>> followers(@RequestHeader(value="Authorization",required=false) String header){User me=auth.resolveUser(header);return connections.findByTargetAndKindAndStatus(me,"FOLLOW","ACCEPTED").stream().map(c->userCard(c.getSource())).toList();}
    @GetMapping("/following") @Transactional(readOnly=true)
    public List<Map<String,Object>> following(@RequestHeader(value="Authorization",required=false) String header){User me=auth.resolveUser(header);return connections.findBySourceAndKindAndStatus(me,"FOLLOW","ACCEPTED").stream().map(c->userCard(c.getTarget())).toList();}
    @GetMapping("/friends") @Transactional(readOnly=true)
    public List<Map<String,Object>> friends(@RequestHeader(value="Authorization",required=false) String header){User me=auth.resolveUser(header);List<Map<String,Object>> result=new ArrayList<>();connections.findBySourceAndKindAndStatus(me,"FRIEND","ACCEPTED").forEach(c->result.add(userCard(c.getTarget())));connections.findByTargetAndKindAndStatus(me,"FRIEND","ACCEPTED").forEach(c->result.add(userCard(c.getSource())));return result;}

    @PostMapping("/block/{id}") @Transactional
    public void block(@RequestHeader(value="Authorization",required=false) String header,@PathVariable Long id){User me=auth.resolveUser(header),target=other(id,me);if(!connections.existsBySourceAndTargetAndKind(me,target,"BLOCK"))connections.save(new SocialConnection(me,target,"BLOCK","ACCEPTED"));removeAllConnections(me,target);}
    @DeleteMapping("/block/{id}") @Transactional
    public void unblock(@RequestHeader(value="Authorization",required=false) String header,@PathVariable Long id){User me=auth.resolveUser(header),target=other(id,me);connections.findBySourceAndTargetAndKind(me,target,"BLOCK").ifPresent(connections::delete);}

    @PostMapping("/posts/{id}/like") @Transactional
    public Map<String,Object> toggleLike(@RequestHeader(value="Authorization",required=false) String header,@PathVariable Long id){User me=auth.resolveUser(header);ProfilePost post=posts.findById(id).orElseThrow(()->new BadRequestException("Publicação não encontrada."));assertCanView(me,post.getUser());Optional<PostLike> existing=likes.findByPostAndUser(post,me);if(existing.isPresent())likes.delete(existing.get());else likes.save(new PostLike(post,me));return Map.of("liked",existing.isEmpty(),"likes",likes.countByPost(post));}
    @GetMapping("/posts/{id}/comments") @Transactional(readOnly=true)
    public List<Map<String,Object>> listComments(@RequestHeader(value="Authorization",required=false) String header,@PathVariable Long id){User me=auth.resolveUser(header);ProfilePost post=posts.findById(id).orElseThrow(()->new BadRequestException("Publicação não encontrada."));assertCanView(me,post.getUser());return comments.findTop100ByPostOrderByCreatedAtAsc(post).stream().map(this::commentMap).toList();}
    @PostMapping("/posts/{id}/comments") @Transactional @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> addComment(@RequestHeader(value="Authorization",required=false) String header,@PathVariable Long id,@RequestBody Map<String,Object> request){User me=auth.resolveUser(header);ProfilePost post=posts.findById(id).orElseThrow(()->new BadRequestException("Publicação não encontrada."));assertCanView(me,post.getUser());String content=Objects.toString(request.get("content"),"").trim();if(content.isBlank()||content.length()>600)throw new BadRequestException("O comentário deve ter até 600 caracteres.");PostComment parent=null;if(request.get("parentId") instanceof Number n){parent=comments.findById(n.longValue()).orElseThrow(()->new BadRequestException("Comentário não encontrado."));if(!parent.getPost().getId().equals(id)||parent.getParent()!=null)throw new BadRequestException("Respostas só podem ser vinculadas a um comentário principal desta publicação.");}return commentMap(comments.save(new PostComment(post,me,parent,content)));}

    @PutMapping
    @Transactional
    public Map<String,Object> update(@RequestHeader(value="Authorization",required=false) String header, @RequestBody ProfileUpdateRequest request) {
        User u=auth.resolveUser(header);
        if(request==null || (request.bio()!=null && request.bio().length()>500)) throw new BadRequestException("A bio pode ter no máximo 500 caracteres.");
        List<String> tags=request.tags()==null?List.of():request.tags().stream().filter(Objects::nonNull).map(String::trim).filter(s->!s.isBlank()).distinct().limit(5).toList();
        if(request.tags()!=null && (request.tags().size()>5 || tags.size()!=request.tags().size())) throw new BadRequestException("Escolha até 5 tags válidas.");
        String visibility=Optional.ofNullable(request.visibility()).orElse("PUBLIC").toUpperCase(Locale.ROOT);
        if(!Set.of("PUBLIC","PRIVATE","FRIENDS").contains(visibility)) throw new BadRequestException("Visibilidade inválida.");
        String profileImage=request.profileImage()==null?null:validateProfileImage(request.profileImage());
        u.updateProfile(request.bio()==null?null:request.bio().trim(),String.join(",",tags),visibility);
        if(request.profileImage()!=null) u.updateProfileImage(profileImage);
        users.save(u);
        return mine(header);
    }

    private String validateProfileImage(String image) {
        if(image.isBlank()) return null;
        if(image.length()>MAX_PROFILE_IMAGE_LENGTH || !image.startsWith(PROFILE_IMAGE_PREFIX)) throw new BadRequestException("A foto deve ser um JPEG com no máximo 96 KB.");
        byte[] decoded;
        try {
            decoded=Base64.getDecoder().decode(image.substring(PROFILE_IMAGE_PREFIX.length()));
        } catch(IllegalArgumentException exception) {
            throw new BadRequestException("A imagem enviada é inválida.");
        }
        if(decoded.length==0 || decoded.length>MAX_PROFILE_IMAGE_BYTES) throw new BadRequestException("A foto deve ter no máximo 96 KB após a compressão.");
        if(decoded.length<3 || decoded[0]!=(byte)0xFF || decoded[1]!=(byte)0xD8 || decoded[2]!=(byte)0xFF) throw new BadRequestException("A imagem JPEG enviada é inválida.");
        return image;
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String,Object> createPost(@RequestHeader(value="Authorization",required=false) String header,@RequestBody ProfilePostRequest request) {
        User u=auth.resolveUser(header); String content=request==null?null:request.content();
        if(content==null || content.isBlank() || content.trim().length()>1200) throw new BadRequestException("A publicação deve ter entre 1 e 1200 caracteres.");
        return postMap(posts.save(new ProfilePost(u,content.trim())),u);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deletePost(@RequestHeader(value="Authorization",required=false) String header,@PathVariable Long id) {
        User u=auth.resolveUser(header); ProfilePost p=posts.findById(id).orElseThrow(()->new BadRequestException("Publicação não encontrada."));
        if(!p.getUser().getId().equals(u.getId())) throw new UnauthorizedException("Você só pode excluir suas próprias publicações.");
        likes.deleteByPost(p); comments.deleteByPostAndParentIsNotNull(p); comments.deleteByPostAndParentIsNull(p);
        posts.delete(p);
    }
    private Map<String,Object> postMap(ProfilePost p,User viewer) { Map<String,Object> m=new LinkedHashMap<>();m.put("id",p.getId());m.put("content",p.getContent());m.put("createdAt",p.getCreatedAt());m.put("likes",likes.countByPost(p));m.put("liked",likes.existsByPostAndUser(p,viewer));m.put("comments",comments.findTop100ByPostOrderByCreatedAtAsc(p).size());return m; }
    private Map<String,Object> equippedCosmeticMap(br.com.ascend.the_system.domain.EquippedCosmetic cosmetic) { Map<String,Object> m=new LinkedHashMap<>();var item=cosmetic.getItem();m.put("slot",cosmetic.getSlot());m.put("id",item.getId());m.put("name",item.getName());m.put("category",item.getCategory());m.put("rarity",item.getRarity());m.put("previewUrl",item.getPreviewUrl());return m; }
    private Map<String,Object> commentMap(PostComment c){Map<String,Object> m=new LinkedHashMap<>();m.put("id",c.getId());m.put("parentId",c.getParent()==null?null:c.getParent().getId());m.put("content",c.getContent());m.put("createdAt",c.getCreatedAt());m.put("userId",c.getUser().getId());m.put("name",c.getUser().getName());m.put("username",c.getUser().getUsername());return m;}
    private Map<String,Object> userCard(User u){return Map.of("id",u.getId(),"name",u.getName(),"username",u.getUsername(),"avatar",u.getAvatar()==null?"":u.getAvatar());}
    private User other(Long id,User me){if(me.getId().equals(id))throw new BadRequestException("Não é possível criar relação consigo mesmo.");if(blocked(me,users.findById(id).orElseThrow(()->new BadRequestException("Jogador não encontrado."))))throw new BadRequestException("Ação indisponível.");return users.findById(id).orElseThrow(()->new BadRequestException("Jogador não encontrado."));}
    private boolean areFriends(User a,User b){return connections.existsBySourceAndTargetAndKindAndStatus(a,b,"FRIEND","ACCEPTED")||connections.existsBySourceAndTargetAndKindAndStatus(b,a,"FRIEND","ACCEPTED");}
    private String friendStatus(User me,User other){if(areFriends(me,other))return "ACCEPTED";if(connections.existsBySourceAndTargetAndKindAndStatus(me,other,"FRIEND","PENDING"))return "OUTGOING";if(connections.existsBySourceAndTargetAndKindAndStatus(other,me,"FRIEND","PENDING"))return "INCOMING";return "NONE";}
    private boolean blocked(User a,User b){return connections.existsBySourceAndTargetAndKind(a,b,"BLOCK")||connections.existsBySourceAndTargetAndKind(b,a,"BLOCK");}
    private void assertCanView(User viewer,User author){if(blocked(viewer,author))throw new BadRequestException("Este conteúdo não está disponível.");if(viewer.getId().equals(author.getId()))return;if(author.getProfileVisibility().equals("PRIVATE")||(author.getProfileVisibility().equals("FRIENDS")&&!areFriends(viewer,author)))throw new BadRequestException("Este perfil é visível somente para amigos.");}
    private void removeAllConnections(User a,User b){for(String kind:List.of("FOLLOW","FRIEND")){connections.findBySourceAndTargetAndKind(a,b,kind).ifPresent(connections::delete);connections.findBySourceAndTargetAndKind(b,a,kind).ifPresent(connections::delete);}}
    private List<String> splitTags(String value) { return value==null||value.isBlank()?List.of():Arrays.asList(value.split(",")); }
}
