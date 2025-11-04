package de.oth.kleinanzeigen.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class GridFsService {

    @Autowired
    private GridFsTemplate gridFsTemplate;

    public String store(MultipartFile file) throws IOException {
        return gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(), file.getContentType())
                .toString();
    }

    public GridFSFile get(String id) {
        return gridFsTemplate.findOne(new Query(Criteria.where("_id").is(id)));
    }
}
