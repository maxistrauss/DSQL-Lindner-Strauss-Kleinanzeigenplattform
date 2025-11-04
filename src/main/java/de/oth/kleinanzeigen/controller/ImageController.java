package de.oth.kleinanzeigen.controller;

import com.mongodb.client.gridfs.model.GridFSFile;
import de.oth.kleinanzeigen.service.GridFsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;

@Controller
public class ImageController {

    @Autowired
    private GridFsService gridFsService;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @GetMapping("/images/{id}")
    public ResponseEntity<InputStreamResource> getImage(@PathVariable String id) throws IOException {
        GridFSFile file = gridFsService.get(id);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        GridFsResource resource = gridFsTemplate.getResource(file);
        return ResponseEntity.ok().contentType(MediaType.valueOf(file.getMetadata().get("_contentType").toString()))
                .body(new InputStreamResource(resource.getInputStream()));
    }
}
