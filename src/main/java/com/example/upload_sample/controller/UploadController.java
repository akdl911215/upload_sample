package com.example.upload_sample.controller;


import com.example.upload_sample.dto.UploadResultDTO;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnailator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@Log4j2
@RequestMapping("/movie/")
@CrossOrigin("*")
public class UploadController {

    @Value("${movie.upload.path}")
    private String uploadPath;

    @Transactional
    @DeleteMapping("/removeFile")
    public ResponseEntity<Boolean> removeFile(String fileName) {

        String srcFileName = null;

        try {

            srcFileName = URLDecoder.decode(fileName, "UTF-8");
            File file = new File(uploadPath + File.separator + srcFileName);
            log.info("file : " + file);
            boolean result = file.delete();
            if (!result) return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);

            File thumbnail = new File(file.getParent(), "s_" + file.getName());

            result = thumbnail.delete();

            return new ResponseEntity<>(result, HttpStatus.OK);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @GetMapping("/display")
    public ResponseEntity<byte[]> getFile(String fileName) {
        log.info("fileName : " + fileName);

        ResponseEntity<byte[]> result = null;
        log.info("up result : " + result);

        try {
            String srcFileName = URLDecoder.decode(fileName, "UTF-8");
            log.info("fileName : " + srcFileName);

            File file = new File(uploadPath + File.separator + srcFileName);
            log.info("file : " + file);


            HttpHeaders header = new HttpHeaders();

            // MIME 타입 처리
            header.add("Content-Type", Files.probeContentType(file.toPath()));
            log.info("header : " + header);

            // 파일 데이터 처리
            result = new ResponseEntity<>(FileCopyUtils.copyToByteArray(file), header, HttpStatus.OK);
            log.info("result : " + result);
        } catch (Exception e) {
            log.info("check");
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("result : " + result);
        return result;
    }

    @PostMapping("/upload")
    public ResponseEntity<List<UploadResultDTO>> uploadFile(MultipartFile[] uploadFiles) {
        log.info("start");

        List<UploadResultDTO> resultDTOList = new ArrayList<>();

        log.info("isEmpty : " + uploadFiles.length);
        for (MultipartFile uploadFile: uploadFiles) {

            // 이미지 파일만 업로드 가능
            if (uploadFile.getContentType().startsWith("image") == false) {
                log.warn("this file is not image type");
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            // 실제 파일 이름 IE나 Edge는 전체 경로가 들어오므로
            String originalName = uploadFile.getOriginalFilename();
            log.info("originalName : " + originalName);
            String fileName = originalName.substring(originalName.lastIndexOf("\\") + 1);

            log.info("fileName : " + fileName);

            // 날짜 폴더 생성
            String folderPath = makeFolder();
            log.info("folderPath : " + folderPath);

            // UUID
            String uuid = UUID.randomUUID().toString();
            log.info("uuid : " + uuid);

            // 저장할 파일 이름 중간에 "_"를 이용해서 구분
            String saveName = uploadPath + File.separator + folderPath + File.separator + uuid + "_" + fileName;
            // File.separator > unix / or window \\ 구분자 추가됨
            log.info("saveName : " + saveName);

            Path savePath = Paths.get(saveName);
            log.info("savePath : " + savePath);

            try {
                uploadFile.transferTo(savePath); // 실제 이미지 저장

                // 섬네일 생성
                String thumbnailSaveName = uploadPath + File.separator + folderPath + File.separator + "s_" + uuid + "_" + fileName;
                // 섬네일 파일 이름은 중간에 s_로 시작하도록
                File thumbnailFile = new File(thumbnailSaveName);
                // 섬네일 생성
                Thumbnailator.createThumbnail(savePath.toFile(), thumbnailFile, 100, 100);

                resultDTOList.add(new UploadResultDTO(fileName, uuid, folderPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new ResponseEntity<>(resultDTOList, HttpStatus.OK);
    }

    private String makeFolder() {

        String str = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        String folderPath = str.replace("/", File.separator);

        // make folder
        File uploadFolder = new File(uploadPath, folderPath);

        if (uploadFolder.exists() == false) {
            uploadFolder.mkdirs();
        }

        return folderPath;
    }

}
