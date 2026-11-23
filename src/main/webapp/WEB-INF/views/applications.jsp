<%@ page pageEncoding="UTF-8" %>
<%@ include file="header.jsp" %>
<h1 class="page-title"><%=staff?"Talent, taking the first step.":"Your journey, in progress."%></h1>
<p class="page-subtitle"><%=staff?"Review applications and keep candidates informed.":"Follow every application from the first step to the next chapter."%></p>
<div class="panel">
  <div class="table-responsive">
    <table class="table align-middle">
      <thead>
        <tr>
          <th>Opportunity</th>
          <% if(staff){ %>
          <th>Candidate</th>
          <% } %>
          <th>Applied</th>
          <th>Status</th>
          <% if(staff){ %>
          <th>Manage</th>
          <% } %>
        </tr>
      </thead>
      <tbody>
        <%
          var applications=(List<Map<String,Object>>)request.getAttribute("applications");
          for(var a:applications){
        %>
        <tr>
          <td><strong><%=Security.html(a.get("title"))%></strong><small
            class="d-block text-secondary"><%=Security.html(a.get("company"))%></small></td>
          <% if(staff){ %>
          <td><%=Security.html(a.get("name"))%><small
            class="d-block"><%=Security.html(a.get("email"))%></small><% if(a.get("resume")!=null){ %><a
            class="small" href="<%=base%>/resume?student=<%=a.get("student_id")%>">Download resume</a><% } %></td>
          <% } %>
          <td class="small"><%=Security.html(a.get("created_at").toString().substring(0,10))%></td>
          <td><span class="status-tag"><%=Security.html(a.get("status"))%></span></td>
          <% if(staff){ %>
          <td>
            <form action="<%=base%>/status" method="post" class="status-form">
              <input type="hidden" name="csrf" value="<%=Security.html(session.getAttribute("csrf"))%>"><input
                type="hidden" name="application" value="<%=a.get("id")%>"><label
                class="visually-hidden" for="status-<%=a.get("id")%>">Application status</label><select
                id="status-<%=a.get("id")%>" class="form-select form-select-sm" name="status"><% for(String st:List.of("Applied","Under Review","Shortlisted","Selected","Rejected")){ %><option
                <%=st.equals(a.get("status"))?"selected":""%>><%=st%></option><% } %></select><button
                class="btn btn-sm btn-outline-primary">Update</button>
            </form>
          </td>
          <% } %>
        </tr>
        <% }if(applications.isEmpty()){ %>
        <tr>
          <td colspan="5" class="empty">No applications yet. New applications will appear here.</td>
        </tr>
        <% } %>
      </tbody>
    </table>
  </div>
</div>
<%@ include file="pagination.jsp" %>
<%@ include file="footer.jsp" %>
