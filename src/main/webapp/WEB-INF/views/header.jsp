<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" import="java.util.*,edu.campus.Security" %>
<%
  Map<String,Object> user=(Map<String,Object>)request.getAttribute("user");
  String base=request.getContextPath()+"/app";
  String assets=request.getContextPath()+"/assets";
  String active=String.valueOf(request.getAttribute("active"));
  String role=user==null?"":(String)user.get("role");
  boolean staff=role.equals("admin")||role.equals("recruiter");
%>
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="theme-color" content="#292343">
    <title>Launchpad | Placement & Internship Portal</title>
    <link rel="stylesheet" href="<%=assets%>/bootstrap.min.css">
    <link rel="stylesheet" href="<%=assets%>/style.css">
    <script src="<%=assets%>/bootstrap.bundle.min.js" defer></script>
    <script src="<%=assets%>/app.js" defer></script>
  </head>
  <body>
    <% if(user!=null){ %>
    <aside class="sidebar" id="sidebar">
      <a class="brand" href="<%=base%>/dashboard"><span class="brand-icon">✦</span> launchpad</a>
      <p class="nav-label">YOUR NEXT CHAPTER</p>
      <nav aria-label="Main navigation">
        <a class="<%=active.equals("/dashboard")?"active":""%>" href="<%=base%>/dashboard">◈ <span>Overview</span></a><a
          class="<%=active.equals("/jobs")?"active":""%>" href="<%=base%>/jobs">▦ <span>Explore opportunities</span></a><a
          class="<%=active.equals("/applications")?"active":""%>" href="<%=base%>/applications">▤ <span><%=staff?"Applications":"My applications"%></span></a><% if(role.equals("student")){ %><a
          class="<%=active.equals("/profile")?"active":""%>" href="<%=base%>/profile">○ <span>My profile & resume</span></a><% } %><% if(staff){ %><a
          class="<%=active.equals("/new-job")?"active":""%>" href="<%=base%>/new-job">⊕ <span>Post an opportunity</span></a><% } %><% if(role.equals("admin")){ %><a
          class="<%=active.equals("/users")?"active":""%>" href="<%=base%>/users">♧ <span>People</span></a><% } %>
      </nav>
      <div class="sidebar-end">
        <p>Potential meets<br>opportunity.</p>
        <small>Your future starts with a first step.</small>
        <form action="<%=base%>/logout" method="post" class="mt-4">
          <input type="hidden" name="csrf" value="<%=Security.html(session.getAttribute("csrf"))%>"><button
            class="btn btn-outline-light w-100">Sign out</button>
        </form>
      </div>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <button class="btn btn-light d-lg-none" id="menuToggle" aria-label="Toggle navigation" aria-expanded="false">☰</button><span>Career centre <span class="text-muted">/ Your workspace</span></span>
        <div class="account">
          <span class="avatar"><%=Security.html(user.get("name").toString().substring(0,1))%></span>
          <div>
            <strong><%=Security.html(user.get("name"))%></strong><small><%=Security.html(role)%></small>
          </div>
        </div>
      </header>
      <main>
        <% if(request.getParameter("saved")!=null){ %>
        <div class="alert alert-success" role="status">Your changes have been saved.</div>
        <% } %>
<% } %>
