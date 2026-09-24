package com.jzo2o.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.api.customer.dto.response.AddressBookResDTO;
import com.jzo2o.api.publics.MapApi;
import com.jzo2o.api.publics.dto.response.LocationResDTO;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.common.utils.BeanUtils;
import com.jzo2o.common.utils.CollUtils;
import com.jzo2o.common.utils.NumberUtils;
import com.jzo2o.common.utils.StringUtils;
import com.jzo2o.customer.mapper.AddressBookMapper;
import com.jzo2o.customer.model.domain.AddressBook;
import com.jzo2o.customer.model.dto.request.AddressBookPageQueryReqDTO;
import com.jzo2o.customer.model.dto.request.AddressBookUpsertReqDTO;
import com.jzo2o.customer.service.IAddressBookService;
import com.jzo2o.mvc.utils.UserContext;
import com.jzo2o.mysql.utils.PageUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 地址薄 服务实现类
 * </p>
 *
 * @author itcast
 * @since 2023-07-06
 */
@Service
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements IAddressBookService {

    @Resource
    private MapApi mapApi;

    @Override
    public List<AddressBookResDTO> getByUserIdAndCity(Long userId, String city) {

        List<AddressBook> addressBooks = lambdaQuery()
                .eq(AddressBook::getUserId, userId)
                .eq(AddressBook::getCity, city)
                .list();
        if(CollUtils.isEmpty(addressBooks)) {
            return new ArrayList<>();
        }
        return BeanUtils.copyToList(addressBooks, AddressBookResDTO.class);
    }

    /**
     * 添加地址薄
     *
     * @param addressBookUpsertReqDTO 地址薄增改请求参数
     */
    @Override
    @Transactional
    public void addAddressBook(AddressBookUpsertReqDTO addressBookUpsertReqDTO) {
        //由于前端和@Valid注解都对DTO进行了校验，这里为方便不再重复校验
        //1.转换数据库对象，补全剩下的属性（登录用户id，是否删除，通过高德地图获取经纬度）
        AddressBook addressBook = BeanUtil.copyProperties(addressBookUpsertReqDTO, AddressBook.class);
        //2.获取当前登录用户id和经纬度，由于AddressBookUpsertReqDTO中的location经纬度不是必需的，这里调用高德地图API获取
        Long userId = UserContext.currentUserId();
        //高德地图的地理编码接口，通过详细地址得到经纬度。
        LocationResDTO locationByAddress = mapApi.getLocationByAddress(addressBookUpsertReqDTO.getAddress());
        String location = locationByAddress.getLocation();
        Double longtitude = Double.valueOf(location.split(",")[0]);
        Double latitude = Double.valueOf(location.split(",")[1]);
        
        addressBook.setUserId(userId);
        addressBook.setIsDeleted(0);//尽管数据库做了默认0，但这里还是显式地设置
        addressBook.setLon(longtitude);
        addressBook.setLat(latitude);

        //3.默认地址处理，查询当前用户的地址簿列表中是否已经存在未删除的默认地址，若存在且前端传递的DTO中的isDefault为1，则设置当前地址为默认地址
        if (addressBookUpsertReqDTO.getIsDefault() == 1) {
            AddressBook addressBookDB = lambdaQuery()
                    .eq(AddressBook::getUserId, userId)
                    .eq(AddressBook::getIsDeleted, 0)
                    .eq(AddressBook::getIsDefault, 1)
                    .one();
            //如果有默认地址，则将其改为非默认
            if (ObjectUtil.isNotNull(addressBookDB)) {
                addressBookDB.setIsDefault(0);
                updateById(addressBookDB);
            }
        }
        //4.保存地址薄
        save(addressBook);
    }
}
