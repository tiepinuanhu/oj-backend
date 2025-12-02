package com.wxc.oj.openFeign;

import com.wxc.oj.model.dto.user.ImgbbResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "imgbb", url = "https://api.imgbb.com")
public interface ImgBBFeignClient {

	@PostMapping(value = "/1/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ImgbbResponse uploadImg(
			@RequestParam("key") String key,
			@RequestPart("image") MultipartFile image);
}