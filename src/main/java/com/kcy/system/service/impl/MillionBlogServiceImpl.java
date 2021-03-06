package com.kcy.system.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.kcy.common.constant.RedisConst;
import com.kcy.common.constant.WebConst;
import com.kcy.common.model.PageConst;
import com.kcy.common.model.ResponseUtils;
import com.kcy.common.model.ResponseWrapper;
import com.kcy.common.redis.RedisService;
import com.kcy.common.utils.BlogUtils;
import com.kcy.common.utils.DateUtils;
import com.kcy.common.utils.IPUtils;
import com.kcy.common.utils.Misc;
import com.kcy.system.dao.MillionBlogMapper;
import com.kcy.system.dao.MillionLabelMapper;
import com.kcy.system.dao.MillionTypeMapper;
import com.kcy.system.model.MillionBlog;
import com.kcy.system.model.MillionLabel;
import com.kcy.system.model.MillionType;
import com.kcy.system.model.MillionUser;
import com.kcy.system.service.MillionBlogService;
import com.kcy.system.vo.VoBlog;
import lombok.extern.java.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Log
@Service
public class MillionBlogServiceImpl implements MillionBlogService {

    @Autowired
    private MillionBlogMapper millionBlogMapper;

    @Autowired
    private MillionLabelMapper millionLabelMapper;

    @Autowired
    private MillionTypeMapper millionTypeMapper;

    @Autowired
    private RedisService redisService;

    /**
     * 发表博客
     * */
    @Override
    public ResponseWrapper releaseBlog(MillionBlog millionBlog, HttpServletRequest request) {
        MillionUser loginUser = BlogUtils.getLoginUser(request);
        if(loginUser == null) {
            return ResponseUtils.errorResponse("未登录");
        }
        MillionType millionType = millionTypeMapper.selectByPrimaryKey(millionBlog.getTypeid());
        if(millionType == null || !"1".equals(millionType.getStatus())) {
            return ResponseUtils.errorResponse("选择类型已下线");
        }
        if(Misc.isStringEmpty(millionBlog.getTitle())) {
            return ResponseUtils.errorResponse("标题不能为空");
        }
        if(Misc.getStringLength(millionBlog.getTitle(),Misc.ISOCODE) > WebConst.MAX_TITLE_COUNT) {
            return ResponseUtils.errorResponse("标题字数过长");
        }
        if(Misc.getStringLength(millionBlog.getContent(),Misc.ISOCODE) > WebConst.MAX_TEXT_COUNT) {
            return ResponseUtils.errorResponse("内容字数过长");
        }
        if(millionBlog.getIseval() == null) {
            millionBlog.setIseval("0");
        } else {
            millionBlog.setIseval("1");
        }
        Document parse = Jsoup.parse(millionBlog.getContent());
        Elements img = parse.body().getElementsByTag("img");
        if(img.size() > 0) {
            millionBlog.setImage(img.get(0).attr("src"));
        }
        millionBlog.setUserid(loginUser.getId());
        millionBlog.setViewnum(0);
        millionBlog.setEvalnum(0);
        millionBlog.setCreatetime(new Date());
        String cropcontent = Misc.excludeHtmlTags(millionBlog.getContent());
        millionBlog.setCropcontent(BlogUtils.getFirstWord(cropcontent) + "...");
        millionBlog.setIp(IPUtils.getIpAddrByRequest(request));
        millionBlog.setTypename(millionType.getName());
        //为了查询方便.......好low啊
        millionBlog.setLabelids(millionBlog.getLabelids()+",");
        millionBlogMapper.insertSelective(millionBlog);
        redisService.remove(RedisConst.MENU_BLOG, RedisConst.INDEX_RESPONSEWRAPPER, RedisConst.ARCHIVES_BLOG);
        log.info(millionBlog.toString());
        return ResponseUtils.successResponse("发表成功");
    }

    /**
     * 获取博客页面数据
     * */
    @Override
    public ResponseWrapper getBlogPageData() {
        ResponseWrapper responseWrapper = ResponseUtils.successResponse("success");
        List <MillionLabel> millionLabels = millionLabelMapper.findAll(null);
        List <MillionType> millionTypes = millionTypeMapper.findAll(null);
        responseWrapper.addAttribute("labels", millionLabels == null ? new ArrayList<MillionLabel>() : millionLabels);
        responseWrapper.addAttribute("types", millionTypes == null ? new ArrayList<MillionType>() : millionTypes);
        return responseWrapper;
    }

    /**
     * 获取博客数据
     * */
    @Override
    public ResponseWrapper findAll(Map<String, Object> param) {
        Integer pageNo = Misc.getIntegerPage(Misc.getString(param.get("page")));
        ResponseWrapper responseWrapper = ResponseUtils.successResponse("success");
        Page <Object> page = PageHelper.startPage(pageNo, PageConst.rows);
        List <MillionBlog> millionBlogs = millionBlogMapper.findAll(param);
        List<VoBlog> voBlogs = new ArrayList();
        for(MillionBlog millionBlog : millionBlogs) {
            VoBlog voBlog = new VoBlog();
            voBlog.setId(millionBlog.getId());
            voBlog.setTitle(millionBlog.getTitle());
            voBlog.setContent(millionBlog.getCropcontent());
            voBlog.setCreatetime(DateUtils.formatDate(millionBlog.getCreatetime()));
            voBlog.setEvalnum(millionBlog.getEvalnum() + " 条评论");
            voBlog.setViewnum(millionBlog.getViewnum() + " 次阅读");
            voBlog.setImg(millionBlog.getImage());
            voBlog.setType(millionBlog.getTypename());
            voBlogs.add(voBlog);
        }
        responseWrapper.addAttribute("datas", voBlogs);
        responseWrapper.addAttribute("query", param);
        responseWrapper.setTotalpage(page.getPages());
        responseWrapper.setPage(pageNo);
        return responseWrapper;
    }

}
