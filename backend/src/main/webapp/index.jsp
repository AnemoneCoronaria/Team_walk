<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>산책시루 서버 테스트</title>
</head>
<body>
  <h1>산책시루 서버가 동작합니다</h1>
  <c:set var="check" value="JSTL 정상 동작" />
  <p>JSP: 정상 동작</p>
  <p>JSTL: <c:out value="${check}" /></p>
  <p>Servlet 테스트: <a href="${pageContext.request.contextPath}/api/hello">/api/hello</a></p>
</body>
</html>