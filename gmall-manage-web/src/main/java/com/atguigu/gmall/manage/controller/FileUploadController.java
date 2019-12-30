package com.atguigu.gmall.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author GPX
 * @date 2019/12/27 21:06
 */
@RestController
@CrossOrigin
public class FileUploadController {


    @Value("${fileServer.url}")
    String fileUrl;

    @RequestMapping(value = "fileUpload", method = RequestMethod.POST)
    public String fileUpload(@RequestParam("file") MultipartFile file) throws IOException, MyException {
        String imgUrl = fileUrl;
        if (file != null) {
            System.out.println("multipartFile = " + file.getName() + "|" + file.getSize());

            //启动配置类 和上传的文件
            String configFile = this.getClass().getResource("/tracker.conf").getFile();
            //初始化配置类
            ClientGlobal.init(configFile);
            //追踪客户端--用于启动
            TrackerClient trackerClient = new TrackerClient();

            //获取连接
            TrackerServer trackerServer = trackerClient.getConnection();
            //存储的服务
            StorageClient storageClient = new StorageClient(trackerServer, null);

            //获取文件名
            String filename = file.getOriginalFilename();
            //截取文件名 用作地址的拼接  后缀名
            String extName = StringUtils.substringAfterLast(filename, ".");

            //拼接地址  file.getBytes() 获取 浏览器的上传地址  用于上传的文件流 file.getBytes()
            String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
            imgUrl = fileUrl;
            for (int i = 0; i < upload_file.length; i++) {
                String path = upload_file[i];
                imgUrl += "/" + path;
            }
            System.out.println(fileUrl);
        }

        return imgUrl;
    }

}
