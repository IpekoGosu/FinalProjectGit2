package com.multi.bbs.together.model.service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Vector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.multi.bbs.board.model.vo.BoardCategory;
import com.multi.bbs.member.model.vo.Member;
import com.multi.bbs.together.model.repository.TogetherBoardAttachFileRepository;
import com.multi.bbs.together.model.repository.TogetherBoardCategoryRepository;
import com.multi.bbs.together.model.repository.TogetherBoardReplyRepository;
import com.multi.bbs.together.model.repository.TogetherBoardRepository;
import com.multi.bbs.together.model.repository.TogetherPhotoRepository;
import com.multi.bbs.together.model.repository.TogetherRepository;
import com.multi.bbs.together.model.vo.TogetherBoard;
import com.multi.bbs.together.model.vo.TogetherBoardAttachFile;
import com.multi.bbs.together.model.vo.TogetherBoardCategory;
import com.multi.bbs.together.model.vo.TogetherBoardParam;
import com.multi.bbs.together.model.vo.TogetherBoardReply;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Service
public class TogetherService {
	@Autowired
	private TogetherBoardRepository boardRepo;
	@Autowired
	private TogetherBoardReplyRepository boardReplyRepo;
	@Autowired
	private TogetherBoardAttachFileRepository boardAttachFileRepository;
	@Autowired
	private TogetherBoardCategoryRepository boardCategoryRepo;
//	@Autowired
//	private TogetherRepository togetherRepo;
//	@Autowired
//	private TogetherPhotoRepository togetherPhotoRepo;
	
//	@Transactional : 쿼리 처리중에 예외 발생시 자동으로 roll-back 시켜주는 어노테이션(AOP)
	@Transactional(rollbackFor = Exception.class)
	public TogetherBoard saveBoard(TogetherBoard board) {
		if(board.getBoardAttachFileList() != null 
				&& board.getBoardAttachFileList().size() > 0 ) {
			for(TogetherBoardAttachFile file : board.getBoardAttachFileList()) {
				boardAttachFileRepository.save(file);
			}
		}
		return boardRepo.save(board);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public TogetherBoardReply saveReply(TogetherBoardReply reply) {
		return boardReplyRepo.save(reply);
	}
	
	
	@Transactional(rollbackFor = Exception.class)
	public TogetherBoard findByNo(int boardNo) {
		TogetherBoard board = boardRepo.findById(boardNo).get();
		board.setReadCount(board.getReadCount() + 1);  
		boardRepo.save(board);
		return board; 
	}
	
	
	@Transactional(rollbackFor = Exception.class)
	public void deleteBoard(int bno, String rootPath) throws Exception {
		TogetherBoard board = this.findByNo(bno);
		
		if(board.getBoardAttachFileList() != null) {
			for(TogetherBoardAttachFile file : board.getBoardAttachFileList()) {
				deleteFile(rootPath, file);
				this.deleteAttachFile(file);
			}
		}
		boardRepo.deleteById(board.getBno());
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void deleteReply(int no) {
		boardReplyRepo.deleteById(no);
	}
	
	//	select
	//    b.*,
	//    m1.*
	//from board b 
	//left outer join member m1 on b.member_m_no = m1.m_no
	//left outer join reply r on r.board_b_no = b.b_no 
	//left outer join member m2 on r.member_m_no = m2.m_no 
	//where
	//b.title like '%갤럭시%' 
	//or b.content like '%갤럭시%'
	//or m1.member_id like '%갤럭시%'
	//or r.content like '%갤럭시%'
	//or m2.member_Id like '%갤럭시%';
	//or (m2.member_Id like '%갤럭시%' and m2.member_Id like '%갤럭시%');
	
	//	https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications
	//	https://starrybleu.github.io/development/2018/07/31/spring-data-jpa-specification.html
	//  https://milenote.tistory.com/137
	public List<TogetherBoard> getSearchAll(String keyword, int page, int limit, List<String> typeList) {
		Specification<TogetherBoard> spec = new Specification<TogetherBoard>() {
			private static final long serialVersionUID = 1L;
//			Root<Board> root or b : 이미 기본적으로 완성된 쿼리, select * from board b 
//			CriteriaBuilder criteriaBuilder : 추가 검색 쿼리 빌딩용 객체
			@Override
			public Predicate toPredicate(Root<TogetherBoard> b, CriteriaQuery<?> query, CriteriaBuilder cb) {
				Join<TogetherBoard, Member> m1 = b.join("member", JoinType.LEFT); // left outer join member m1 on b.member_m_no = m1.m_no 하는 코드
				Join<TogetherBoard, TogetherBoardReply> r = b.join("replyList", JoinType.LEFT); // left outer join reply r on r.board_b_no = b.b_no 
				Join<TogetherBoardReply, Member> m2 = b.join("member", JoinType.LEFT); // left outer join member m2 on r.member_m_no = m2.m_no
				Join<TogetherBoard, BoardCategory> c = b.join("boardCategory", JoinType.LEFT);
				
				Predicate predicate = cb.or(
						cb.like(b.get("title"), "%"+keyword+"%"),
						cb.like(b.get("content"), "%"+keyword+"%"),
						cb.like(m1.get("memberId"), "%"+keyword+"%"),
						cb.like(r.get("content"), "%"+keyword+"%"),
						cb.like(m2.get("memberId"), "%"+keyword+"%")
//						cb.and(cb.like(b.get("title"), ""), cb.like(b.get("title"), ""))
						);
				
				// 동적 쿼리 작성하는 방법
				if(typeList != null && typeList.size() > 0) {
					In<String> inCase = cb.in(c.get("type"));
					for(String type : typeList) {
						inCase.value(type);
					}
					predicate = cb.and(inCase, predicate);
				}
				return predicate;
			}
		};

		Sort sort = Sort.by("boardCategory_level").and(Sort.by("bno").descending());
		PageRequest request = PageRequest.of(page, limit, sort);
		return boardRepo.findAll(spec, request);
	}
	
	
	
	public int getSearchAllCount(String keyword, List<String> typeList) {
		Specification<TogetherBoard> spec = new Specification<TogetherBoard>() {
			private static final long serialVersionUID = 1L;
			public Predicate toPredicate(Root<TogetherBoard> b, CriteriaQuery<?> query, CriteriaBuilder cb) {
				Join<TogetherBoard, Member> m1 = b.join("member", JoinType.LEFT);
				Join<TogetherBoard, TogetherBoardReply> r = b.join("replyList", JoinType.LEFT);
				Join<TogetherBoardReply, Member> m2 = r.join("member", JoinType.LEFT);
				Join<TogetherBoard, TogetherBoardCategory> c = b.join("boardCategory", JoinType.LEFT);
				Predicate predicate = cb.or(
						cb.like(b.get("title"), "%" + keyword + "%"),
						cb.like(b.get("content"), "%" + keyword + "%"),
						cb.like(m1.get("memberId"), "%" + keyword + "%"),
						cb.like(m2.get("memberId"), "%" + keyword + "%"),
						cb.like(r.get("content"), "%" + keyword + "%"));
				
				// 동적 쿼리 작성하는 방법
				if(typeList != null && typeList.size() > 0) {
					In<String> inCase = cb.in(c.get("type"));
					for(String type : typeList) {
						inCase.value(type);
					}
					predicate = cb.and(inCase, predicate);
				}
				query.distinct(true);
				return predicate;
			}
		};
		spec.or(new Specification<TogetherBoard>() {
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<TogetherBoard> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
				query.distinct(true);
				return null;
			}
		});
		return boardRepo.count(spec);
	}
	
	public Vector<TogetherBoardCategory> getBoardCategory() {
		return new Vector<>(boardCategoryRepo.findAllByOrderByOrderno());
	}

	public int getBoardCount(TogetherBoardParam param) {
		if(param.getSearchType().equals("all")) {
			return this.getSearchAllCount(param.getSearchValue(), param.getTypeList());
		} else if(param.getSearchType().equals("title")) {
			if(param.getTypeList() != null && param.getTypeList().size() > 0) {
				return boardRepo.countByTitleContainingAndContentContainingAndMember_MemberIdContainingAndBoardCategory_typeIn(param.getSearchValue(), "", "", param.getTypeList());
			} else {
				return boardRepo.countByTitleContainingAndContentContainingAndMember_MemberIdContaining(param.getSearchValue(), "", "");
			}
		} else if(param.getSearchType().equals("content")) {
			if(param.getTypeList() != null && param.getTypeList().size() > 0) {
				return boardRepo.countByTitleContainingAndContentContainingAndMember_MemberIdContainingAndBoardCategory_typeIn("", param.getSearchValue(), "", param.getTypeList());
			} else {
				return boardRepo.countByTitleContainingAndContentContainingAndMember_MemberIdContaining("", param.getSearchValue(), "");
			}
		} else if(param.getSearchType().equals("writer")) {
			if(param.getTypeList() != null && param.getTypeList().size() > 0) {
				return boardRepo.countByTitleContainingAndContentContainingAndMember_MemberIdContainingAndBoardCategory_typeIn("", "", param.getSearchValue(), param.getTypeList());
			} else {
				return boardRepo.countByTitleContainingAndContentContainingAndMember_MemberIdContaining("", "", param.getSearchValue());
			}
		} else {
			if(param.getTypeList() != null && param.getTypeList().size() > 0) {
				return boardRepo.countByTitleContainingAndContentContainingAndMember_MemberIdContainingAndBoardCategory_typeIn("", "", "", param.getTypeList());
			} else {
				return boardRepo.countByTitleContainingAndContentContainingAndMember_MemberIdContaining("", "", "");
			}
		}
	}
	

	public List<TogetherBoard> getBoardList(TogetherBoardParam param) {
		Sort sort = Sort.by("boardCategory_level").and(Sort.by("bno").descending());
		PageRequest request = PageRequest.of(param.getPage()-1, param.getLimit(), sort);
		
		if(param.getSearchType().equals("all") && (param.getSearchValue() != null && param.getSearchValue().length() > 0)) {
			return this.getSearchAll(param.getSearchValue(), param.getPage()-1, param.getLimit(), param.getTypeList());
		} else if(param.getSearchType().equals("title")) {
			if(param.getTypeList() != null && param.getTypeList().size() > 0) {
				return boardRepo.findByTitleContainingAndContentContainingAndMember_MemberIdContainingAndBoardCategory_typeIn(param.getSearchValue(), "", "", param.getTypeList(), request);
			} else {
				return boardRepo.findByTitleContainingAndContentContainingAndMember_MemberIdContaining(param.getSearchValue(), "", "",request);
			}
		} else if(param.getSearchType().equals("content")) {
			if(param.getTypeList() != null && param.getTypeList().size() > 0) {
				return boardRepo.findByTitleContainingAndContentContainingAndMember_MemberIdContainingAndBoardCategory_typeIn("", param.getSearchValue(), "", param.getTypeList(), request);
			} else {
				return boardRepo.findByTitleContainingAndContentContainingAndMember_MemberIdContaining("", param.getSearchValue(), "",request);
			}
		} else if(param.getSearchType().equals("writer")) {
			if(param.getTypeList() != null && param.getTypeList().size() > 0) {
				return boardRepo.findByTitleContainingAndContentContainingAndMember_MemberIdContainingAndBoardCategory_typeIn("", "", param.getSearchValue(), param.getTypeList(), request);
			} else {
				return boardRepo.findByTitleContainingAndContentContainingAndMember_MemberIdContaining("", "", param.getSearchValue(), request);
			}
		} else {
			if(param.getTypeList() != null && param.getTypeList().size() > 0) {
				return boardRepo.findByTitleContainingAndContentContainingAndMember_MemberIdContainingAndBoardCategory_typeIn("", "", "", param.getTypeList(), request);
			} else {
				return boardRepo.findByTitleContainingAndContentContainingAndMember_MemberIdContaining("", "", "", request);
			}
		}
	}

	public TogetherBoardAttachFile findBoardAttachFile(int fno) {
		return boardAttachFileRepository.findById(fno).get();
	}
	
	public String saveFile(MultipartFile upFile, String savePath) {
		File folder = new File(savePath);
		
		// 폴더 없으면 만드는 코드
		if(folder.exists() == false) {
			folder.mkdirs();
		}
		System.out.println("savePath : " + savePath);
		
		// 파일이름을 랜덤하게 바꾸는 코드, test.txt -> 20221213_1728291212.txt
		String originalFileName = upFile.getOriginalFilename();
		String reNameFileName = 
					LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));
		reNameFileName += originalFileName.substring(originalFileName.lastIndexOf("."));
		String reNamePath = savePath + "/" + reNameFileName;
		
		try {
			// 실제 파일이 저장되는 코드
			upFile.transferTo(new File(reNamePath));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return reNameFileName;
	}

	public void deleteFile(String savePath, TogetherBoardAttachFile boardArrachFile) {
		File file = new File(savePath +"/" + boardArrachFile.getRenamedFilename());
		if(file.exists()) {
			file.delete();
		}
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void deleteAttachFile(TogetherBoardAttachFile arrachFile) {
		boardAttachFileRepository.deleteById(arrachFile.getFno());
	}
	
}


