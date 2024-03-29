package com.multi.bbs.heritage.model.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.multi.bbs.heritage.model.repository.HBookmarkRepository;
import com.multi.bbs.heritage.model.repository.HReviewRepository;
import com.multi.bbs.heritage.model.repository.HeritageRepository;
import com.multi.bbs.heritage.model.vo.HBookmark;
import com.multi.bbs.heritage.model.vo.HReview;
import com.multi.bbs.heritage.model.vo.Heritage;
import com.multi.bbs.heritage.model.vo.HeritageParam;
import com.multi.bbs.member.model.vo.Member;


@Service
public class HeritageService {
	@Autowired
	private HReviewRepository reviewrepo;
	
	@Autowired
	private HeritageRepository heritageRepository;
	
	@Autowired
	private HBookmarkRepository bmkrepo;
	
	// String ccbaMnm1, String ccbaCtcdNm, String ccmaName
		public List<Heritage> getSearchAll(String keyword, String region, String category, String generation, HeritageParam param){
			Sort sort = Sort.by("hno");
			PageRequest request = PageRequest.of(param.getPage()-1, param.getLimit(), sort);
			
			return heritageRepository.findByCcbaMnm1ContainingAndCcbaCtcdNmContainingAndCcmaNameContainingAndCcceNameContainingOrderByCcbaMnm1(keyword, region, category, generation, request);
		}
		public int getSearchCount(String keyword, String region, String category, String generation){
			return heritageRepository.countByCcbaMnm1ContainingAndCcbaCtcdNmContainingAndCcmaNameContainingAndCcceNameContainingOrderByCcbaMnm1(keyword, region, category, generation);
		}
	
	@Transactional
	public Heritage findByHno(int hno) {
		Heritage heritage = heritageRepository.findByHno(hno);
		heritage.setViewCount(heritage.getViewCount() + 1);
		return heritage;
	}
	@Transactional(rollbackFor = Exception.class)
	public HReview saveReview(HReview hreview) {
		Heritage heritage = hreview.getHeritage();
		heritage.setReviewCount(heritage.getReviewCount() + 1);
		return reviewrepo.save(hreview);
	}
	public List<HReview> reviewList(Heritage heritage) {
		return reviewrepo.findByHeritage(heritage);
	}
	@Transactional
	public void deleteReply(int replyNo) {
		
		Heritage heritage = reviewrepo.findById(replyNo).get().getHeritage();
		if (heritage.getReviewCount() > 0) {
			heritage.setReviewCount(heritage.getReviewCount() - 1);
		}
		reviewrepo.deleteById(replyNo);
	}
	
	@Transactional
	public HBookmark saveBookmark(HBookmark hBookmark) {
		return bmkrepo.save(hBookmark);
	}
	@Transactional
	public void deleteBookmark(int bno) {
		bmkrepo.deleteById(bno);
	}
	public HBookmark findbmkByBno(int bno) {
		return bmkrepo.findByBno(bno);
	}
	public List<HBookmark> findbmkByMno(int mno) {
		return bmkrepo.findByMno(mno);
	}
	public HBookmark findbmkByhnoandmno(int hno, int mno) {
		return bmkrepo.findByHnoAndMno(hno, mno);
	}
	
}
