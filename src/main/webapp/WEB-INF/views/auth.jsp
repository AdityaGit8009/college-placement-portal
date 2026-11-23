<%@ page pageEncoding="UTF-8" %>
<%@ include file="header.jsp" %>
<% boolean registering=Boolean.TRUE.equals(request.getAttribute("register")); %>
<div class="auth-layout">
  <section class="auth-story">
    <a class="brand" href="<%=base%>/login"><span class="brand-icon">✦</span> launchpad</a>
    <div>
      <span class="eyebrow">FROM CAMPUS TO WHAT COMES NEXT</span>
      <h1>A little ambition.<br>A world of<br>possibility.</h1>
      <p>Discover internships, meet your next employer<br>and turn your potential into a first opportunity.</p>
      <div class="story-tags">
        <span>Internships</span><span>Graduate careers</span><span>Your future</span>
      </div>
    </div>
    <p class="small">Your college placement journey, in one place.</p>
  </section>
  <section class="auth-panel">
    <div class="auth-card">
      <span class="eyebrow text-purple">THE NEXT STEP IS YOURS</span>
      <h2><%=registering?"Start your journey.":"Good to see you."%></h2>
      <p class="text-secondary"><%=registering?"Create your student account to get started.":"Sign in to your placement workspace."%></p>
      <form action="<%=base%>/<%=registering?"register":"login"%>" method="post">
        <input type="hidden" name="csrf" value="<%=Security.html(session.getAttribute("csrf"))%>"><% if(registering){ %>
        <div class="mb-3">
          <label class="form-label" for="name">Full name</label><input
            class="form-control" id="name" name="name" required minlength="2" maxlength="80" autocomplete="name">
        </div>
        <% } %>
        <div class="mb-3">
          <label class="form-label" for="email">Email address</label><input
            class="form-control" type="email" id="email" name="email" required maxlength="254" autocomplete="email">
        </div>
        <div class="mb-4">
          <label class="form-label" for="password">Password</label><input
            class="form-control" type="password" id="password" name="password" required minlength="<%=registering?12:1%>" maxlength="100" autocomplete="<%=registering?"new-password":"current-password"%>"><% if(registering){ %><small
            class="text-secondary">Use at least 12 characters.</small><% } %>
        </div>
        <button class="btn btn-primary w-100"><%=registering?"Create student account":"Sign in"%></button>
      </form>
      <p class="text-center mt-4"><a
        href="<%=base%>/<%=registering?"login":"register"%>"><%=registering?"Already registered? Sign in":"New student? Create an account"%></a></p>
      <p class="small text-secondary">Recruiter accounts are issued by the placement officer.</p>
    </div>
  </section>
</div>
<%@ include file="footer.jsp" %>
