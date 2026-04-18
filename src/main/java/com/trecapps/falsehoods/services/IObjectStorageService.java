package com.trecapps.falsehoods.services;

import com.trecapps.falsehoods.models.BrandContent;
import com.trecapps.falsehoods.models.ContentVersion;
import lombok.Data;
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
import java.util.SortedSet;
import java.util.UUID;

@Service
public interface IObjectStorageService {

    @Data
    static class ImageData {
        String bigImage;
        byte[] thumbnail;
        String imageType;
    }

    default ImageData generateThumbnail(String base64Image, int width, int height){

        String[] basePieces = base64Image.split(";base64,");

        ImageData imageData = new ImageData();

        imageData.imageType = basePieces[0].substring(5);
        imageData.bigImage = base64Image;

        // Decode the base64 image
        byte[] bigImage = Base64.getDecoder().decode(basePieces[1]);

        try(ByteArrayInputStream bais = new ByteArrayInputStream(bigImage)){
            BufferedImage originalImage = ImageIO.read(bais);
            if(originalImage == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data!");

            // Create Thumbnail
            BufferedImage thumbnail = Thumbnails.of(originalImage)
                    .size(width, height)
                    .outputFormat(imageData.imageType.substring(6))
                    .asBufferedImage();

            try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
                ImageIO.write(thumbnail, imageData.imageType.substring(6), baos);
                imageData.thumbnail = baos.toByteArray();
                return imageData;
            }
        } catch(IOException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data!");
        }
    }

    default String getFalsehoodId(UUID id){
        return String.format("Falsehood-%s.json", id);
    }

    Mono<String> retrieveThumbnail(UUID id);

    Mono<BrandContent> retrieveBrandContent(UUID id);

    Mono<BrandContent> saveBrandContent(UUID id, BrandContent content);

    Mono<SortedSet<ContentVersion>> persistFalsehoodContent(UUID id, String content);

    Mono<SortedSet<ContentVersion>> getFalsehoodContent(UUID id);
}
