package com.ankkun.ecommerce.module.upload;
import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.common.exception.BadRequestException;
import com.ankkun.ecommerce.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import java.nio.file.*;
import java.util.*;
@RestController @RequiredArgsConstructor
public class UploadController {
    @Value("${app.upload-directory:../../.demo/uploads}")private String directory;
    private String store(MultipartFile file) throws java.io.IOException {
        if(file.isEmpty()||file.getSize()>5*1024*1024)throw new BadRequestException("Ảnh phải từ 1 byte đến 5MB");byte[] bytes=file.getBytes();String ext;
        if(bytes.length>=8&&bytes[0]==(byte)137&&bytes[1]==80&&bytes[2]==78&&bytes[3]==71)ext="png";
        else if(bytes.length>=3&&bytes[0]==(byte)255&&bytes[1]==(byte)216&&bytes[2]==(byte)255)ext="jpg";
        else if(bytes.length>=12&&new String(bytes,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")&&new String(bytes,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP"))ext="webp";
        else throw new BadRequestException("Chỉ hỗ trợ PNG, JPEG và WebP");
        if(ext.equals("webp"))throw new BadRequestException("Demo hỗ trợ PNG và JPEG; hãy chuyển WebP trước khi tải");
        if(!ext.equals("webp")){try(var stream=javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))){var readers=javax.imageio.ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new BadRequestException("Ảnh không hợp lệ");var reader=readers.next();try{reader.setInput(stream);if((long)reader.getWidth(0)*reader.getHeight(0)>40000000)throw new BadRequestException("Ảnh có kích thước quá lớn");}finally{reader.dispose();}}catch(javax.imageio.IIOException ex){throw new BadRequestException("Ảnh không hợp lệ");}}
        Path root=Path.of(directory).toAbsolutePath().normalize();Files.createDirectories(root);String name=UUID.randomUUID()+"."+ext;Files.write(root.resolve(name),bytes,StandardOpenOption.CREATE_NEW);return "/api/files/"+name;
    }
    private final com.ankkun.ecommerce.module.user.service.UserService users;
    @PostMapping("/api/users/me/avatar")public Object avatar(@org.springframework.security.core.annotation.AuthenticationPrincipal String userId,@RequestParam("image")MultipartFile file)throws Exception{String url=store(file);var dto=new com.ankkun.ecommerce.module.user.dto.UpdateProfileDto();dto.setAvatar(url);users.updateProfile(userId,dto);return ApiResponse.ok(url);}
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/admin/upload")public Object product(@RequestParam("file")MultipartFile file)throws Exception{return Map.of("url",store(file));}
    @GetMapping("/api/files/{name}")public ResponseEntity<byte[]> image(@PathVariable String name)throws Exception{
        if(!name.matches("[a-f0-9-]+\\.(png|jpg|webp)"))throw new NotFoundException("Image not found");Path root=Path.of(directory).toAbsolutePath().normalize();Path path=root.resolve(name).normalize();if(!path.startsWith(root)||!Files.isRegularFile(path))throw new NotFoundException("Image not found");
        String type=name.endsWith("png")?"image/png":name.endsWith("jpg")?"image/jpeg":"image/webp";
        return ResponseEntity.ok().header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1))).contentType(MediaType.parseMediaType(type)).body(Files.readAllBytes(path));
    }
}
