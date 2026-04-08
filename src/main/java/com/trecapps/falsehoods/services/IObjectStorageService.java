package com.trecapps.falsehoods.services;

import com.trecapps.falsehoods.models.BrandContent;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Service
public interface IObjectStorageService {

    default String generateThumbnail(String base64Image, int width, int height){
        // Decode the base64 image
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        try(ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)){
            BufferedImage originalImage = ImageIO.read(bais);
            if(originalImage == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data!");

            // Create Thumbnail
            BufferedImage thumbnail = Thumbnails.of(originalImage)
                    .size(width, height)
                    .outputFormat("jpeg")
                    .asBufferedImage();

            try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
                ImageIO.write(thumbnail, "jpeg", baos);
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            }
        } catch(IOException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data!");
        }
    }

    Mono<String> retrieveThumbnail(UUID id);

    Mono<BrandContent> retrieveBrandContent(UUID id);

    Mono<BrandContent> saveBrandContent(UUID id, BrandContent content);
}
