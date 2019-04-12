package com.kcy.system.service.impl;

import com.kcy.common.constant.RedisConst;
import com.kcy.common.model.ResponseUtils;
import com.kcy.common.model.ResponseWrapper;
import com.kcy.common.redis.RedisComponent;
import com.kcy.common.utils.BlogUtils;
import com.kcy.common.utils.DateUtils;
import com.kcy.system.dao.MillionBlogMapper;
import com.kcy.system.dao.MillionWhisperMapper;
import com.kcy.system.model.MillionBlog;
import com.kcy.system.model.MillionWhisper;
import com.kcy.system.service.MillionBlogService;
import com.kcy.system.service.MillionEvaluationService;
import com.kcy.system.service.MillionService;
import com.kcy.system.vo.VoBlog;
import com.kcy.system.vo.VoBlogDetails;
import com.kcy.system.vo.VoIndexMillionWhisper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kcy.common.utils.BlogUtils.checkHitsFrequency;

@Service
public class MillionServiceImpl implements MillionService {

    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private MillionBlogService millionBlogService;
    @Autowired
    private MillionWhisperMapper millionWhisperMapper;
    @Autowired
    private MillionBlogMapper millionBlogMapper;
    @Autowired
    private MillionEvaluationService millionEvaluationService;

    //获取首页信息数据
    public ResponseWrapper getIndexData() {
        ResponseWrapper responseWrapper = (ResponseWrapper)redisComponent.getOpsForObject(RedisConst.INDEX_RESPONSEWRAPPER);
        if(responseWrapper == null) {
            Map<String, Object> param = new HashMap();
            param.put("page", 1);
            responseWrapper = millionBlogService.findAll(param);
            if(responseWrapper == null || !responseWrapper.isSuccess()) {
                responseWrapper = ResponseUtils.successResponse("");
            }
            MillionWhisper newsMessage = millionWhisperMapper.getNewsMessage();
            if(newsMessage != null) {
                VoIndexMillionWhisper voIndexMillionWhisper = new VoIndexMillionWhisper();
                voIndexMillionWhisper.setId(newsMessage.getId());
                voIndexMillionWhisper.setContent(newsMessage.getContent());
                responseWrapper.addAttribute("whisper", voIndexMillionWhisper);
            }
            redisComponent.opsForValue(RedisConst.INDEX_RESPONSEWRAPPER, responseWrapper);
        }
        return responseWrapper;
    }

    //文章详情
    public ResponseWrapper getBlogDetails(HttpServletRequest request, Integer id) {
        MillionBlog millionBlog = millionBlogMapper.selectByPrimaryKey(id);
        if(millionBlog == null || !"1".equals(millionBlog.getStatus())) {
            return ResponseUtils.errorResponse("此博客已下线");
        }
        //控制文章阅读量
        boolean fale = BlogUtils.checkHitsFrequency(request, id, redisComponent);
        if(!fale) {
            millionBlog.setViewnum(millionBlog.getViewnum() + 1);
            millionBlogMapper.updateByPrimaryKeySelective(millionBlog);
        }
        VoBlogDetails voBlogDetails = new VoBlogDetails();
        voBlogDetails.setId(millionBlog.getId());
        voBlogDetails.setTitle(millionBlog.getTitle());
        voBlogDetails.setCreatetime(DateUtils.formatDate(millionBlog.getCreatetime()));
        voBlogDetails.setType(millionBlog.getTypename());
        voBlogDetails.setEvalnum(millionBlog.getEvalnum() + " 条评论");
        voBlogDetails.setViewnum(millionBlog.getViewnum() + " 次阅读");
        voBlogDetails.setContent(millionBlog.getContent());
        voBlogDetails.setIseval(millionBlog.getIseval());

        //获取关于这篇文章的第一页评论数据
        Map<String, Object> param = new HashMap(3);
        param.put("page", 1);
        param.put("blogid", id);
        param.put("evaluation", 0);
        param.put("type", 1);
        ResponseWrapper responseWrapper = millionEvaluationService.getAllByAimsId(param);
        if(responseWrapper == null || !responseWrapper.isSuccess()) {
            responseWrapper = ResponseUtils.successResponse("");
        }
        responseWrapper.addAttribute("blog", voBlogDetails);

        return responseWrapper;
    }
}