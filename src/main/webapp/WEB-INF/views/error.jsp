<%@ page pageEncoding="UTF-8" %>
<%@ include file="header.jsp" %>
<div class="error-page"><span class="eyebrow text-purple">LET'S GET YOU BACK ON TRACK</span><h1>We couldn't complete that.</h1><p role="alert"><%=Security.html(request.getAttribute("message"))%></p><a class="btn btn-primary" href="<%=base%>/dashboard">Return to workspace</a><a class="btn btn-light" href="<%=base%>/login">Sign in</a></div>
<%@ include file="footer.jsp" %>
