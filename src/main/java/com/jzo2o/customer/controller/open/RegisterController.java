package com.jzo2o.customer.controller.open;

import com.jzo2o.customer.model.dto.request.InstitutionRegisterReqDTO;
import com.jzo2o.customer.service.IServeProviderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController("openRegisterController")
@RequestMapping("/open/serve-provider")
@Api(tags = "开放注册接口")
public class RegisterController {

    @Resource
    private IServeProviderService iServeProviderService;

    @PostMapping("/institution/register")
    @ApiOperation("机构注册接口")
    public void institutionRegister(@Valid @RequestBody InstitutionRegisterReqDTO institutionRegisterReqDTO) {
        iServeProviderService.register(institutionRegisterReqDTO);
    }
}
