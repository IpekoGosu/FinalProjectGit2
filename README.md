# 파이널 프로젝트 2조 문화월드

## JQuery 사용법
static>js폴더에 jquery-3.6.0.min.js 파일 사용<br>
html파일에 다음 태그 추가
```
<script th:src="@{/js/jquery-3.6.0.min.js}"></script>
```

## Header, Footer 추가법
```
<th:block th:replace="common/header :: headerFragment"></th:block>
<th:block th:replace="common/footer :: footerFragment"></th:block>
```
