package com.wxc.oj.openFeign;

import com.wxc.oj.model.req.sandbox.Result;
import com.wxc.oj.model.req.sandbox.SandBoxRequest;
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