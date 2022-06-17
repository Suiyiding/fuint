package com.fuint.application.service.balance;

import com.fuint.application.dao.entities.*;
import com.fuint.application.dao.repositories.MtBalanceRepository;
import com.fuint.application.dao.repositories.MtUserRepository;
import com.fuint.application.dto.BalanceDto;
import com.fuint.application.enums.StatusEnum;
import com.fuint.application.service.member.MemberService;
import com.fuint.base.dao.pagination.PaginationRequest;
import com.fuint.base.dao.pagination.PaginationResponse;
import com.fuint.exception.BusinessCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 余额管理业务实现类
 * Created by FSQ
 * Contact wx fsq_better
 * Site https://www.fuint.cn
 */
@Service
public class BalanceServiceImpl implements BalanceService {

    private static final Logger log = LoggerFactory.getLogger(BalanceServiceImpl.class);

    @Autowired
    private MtBalanceRepository balanceRepository;

    @Autowired
    private MtUserRepository userRepository;

    @Autowired
    private MemberService memberService;

    /**
     * 分页查询余额列表
     *
     * @param paginationRequest
     * @return
     */
    @Override
    public PaginationResponse<BalanceDto> queryBalanceListByPagination(PaginationRequest paginationRequest) throws BusinessCheckException {
        PaginationResponse<MtBalance> paginationResponse = balanceRepository.findResultsByPagination(paginationRequest);

        List<BalanceDto> content = new ArrayList<>();
        List<MtBalance> dataList = paginationResponse.getContent();
        for (MtBalance mtBalance : dataList) {
            MtUser userInfo = memberService.queryMemberById(mtBalance.getUserId());
            BalanceDto item = new BalanceDto();
            item.setId(mtBalance.getId());
            item.setAmount(mtBalance.getAmount());
            item.setDescription(mtBalance.getDescription());
            item.setCreateTime(mtBalance.getCreateTime());
            item.setUpdateTime(mtBalance.getUpdateTime());
            item.setUserId(mtBalance.getUserId());
            item.setUserInfo(userInfo);
            item.setOrderSn(mtBalance.getOrderSn());
            item.setStatus(mtBalance.getStatus());
            content.add(item);
        }

        PageRequest pageRequest = new PageRequest((paginationRequest.getCurrentPage() +1), paginationRequest.getPageSize());
        Page page = new PageImpl(content, pageRequest, paginationResponse.getTotalElements());
        PaginationResponse<BalanceDto> result = new PaginationResponse(page, BalanceDto.class);
        result.setTotalPages(paginationResponse.getTotalPages());
        result.setContent(content);

        return result;
    }

    /**
     * 添加余额记录
     *
     * @param mtBalance
     * @throws BusinessCheckException
     */
    @Override
    @Transactional
    public boolean addBalance(MtBalance mtBalance) {
        if (mtBalance.getUserId() < 0) {
           return false;
        }
        mtBalance.setStatus(StatusEnum.ENABLED.getKey());
        mtBalance.setCreateTime(new Date());
        mtBalance.setUpdateTime(new Date());

        MtUser user = userRepository.findOne(mtBalance.getUserId());
        BigDecimal newAmount = user.getBalance().add(mtBalance.getAmount());
        if (newAmount.compareTo(new BigDecimal("0")) < 0) {
           return false;
        }

        user.setBalance(newAmount);
        userRepository.save(user);

        mtBalance.setMobile(user.getMobile());
        balanceRepository.save(mtBalance);

        return true;
    }
}
