package com.wxc.oj.openFeign;

import com.wxc.oj.model.dto.sandbox.Result;
import com.wxc.oj.model.dto.sandbox.SandBoxRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
		value = "sandbox-feign",
		url = "http://${remote.address}:5050"
)
public interface SandboxFeignClient {


	@PostMapping("/run")
	List<Result> run(@RequestBody SandBoxRequest sandBoxRequest);

	@DeleteMapping("/file/{id}")
	void deleteFile(@PathVariable String id);

}