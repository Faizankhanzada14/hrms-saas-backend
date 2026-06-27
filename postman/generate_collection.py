import json

BASE = "{{baseUrl}}/api/v1"

def header_auth():
    return [{"key": "Authorization", "value": "Bearer {{accessToken}}", "type": "text"}]

def req(name, method, path, body=None, params=None, auth=True, body_mode="raw",
        form_data=None, description="", test_script=None):
    url_raw = BASE + path
    query = []
    if params:
        for k, v in params.items():
            query.append({"key": k, "value": str(v)})
        if query:
            url_raw += "?" + "&".join(f"{q['key']}={q['value']}" for q in query)

    url = {
        "raw": url_raw,
        "host": ["{{baseUrl}}"],
        "path": ["api", "v1"] + [p for p in path.strip("/").split("/") if p],
    }
    if query:
        url["query"] = query

    item = {
        "name": name,
        "request": {
            "method": method,
            "header": header_auth() if auth else [],
            "url": url,
            "description": description,
        },
        "response": [],
    }

    if body is not None:
        if body_mode == "raw":
            item["request"]["header"].append({"key": "Content-Type", "value": "application/json"})
            item["request"]["body"] = {
                "mode": "raw",
                "raw": json.dumps(body, indent=2),
                "options": {"raw": {"language": "json"}},
            }
    elif form_data is not None:
        formdata_items = []
        for f in form_data:
            entry = {"key": f["key"], "type": f.get("type", "text")}
            if f.get("type") == "file":
                entry["src"] = []
            else:
                entry["value"] = f.get("value", "")
            formdata_items.append(entry)
        item["request"]["body"] = {"mode": "formdata", "formdata": formdata_items}

    if test_script:
        item["event"] = [{
            "listen": "test",
            "script": {"type": "text/javascript", "exec": test_script},
        }]

    return item

def folder(name, items, description=""):
    return {"name": name, "item": items, "description": description}


# ---------------- Auth ----------------
auth_items = [
    req("Login", "POST", "/auth/login",
        body={"username": "superadmin", "password": "Admin@123"},
        auth=False,
        description="Public. Returns AuthResponse. The test script below auto-saves accessToken/refreshToken as collection variables so every other request in this collection works immediately after running this one.",
        test_script=[
            "const data = pm.response.json();",
            "if (data.accessToken) { pm.collectionVariables.set('accessToken', data.accessToken); }",
            "if (data.refreshToken) { pm.collectionVariables.set('refreshToken', data.refreshToken); }",
        ]),
    req("Refresh Token", "POST", "/auth/refresh",
        body={"refreshToken": "{{refreshToken}}"}, auth=False,
        test_script=[
            "const data = pm.response.json();",
            "if (data.accessToken) { pm.collectionVariables.set('accessToken', data.accessToken); }",
        ]),
    req("Logout", "POST", "/auth/logout", body={"refreshToken": "{{refreshToken}}"}),
    req("Change Password", "POST", "/auth/change-password",
        body={"currentPassword": "Admin@123", "newPassword": "NewAdmin@123"}),
    req("Forgot Password", "POST", "/auth/forgot-password", body={"email": "superadmin@hrms.local"}, auth=False),
    req("Reset Password", "POST", "/auth/reset-password",
        body={"token": "{{passwordResetToken}}", "newPassword": "NewAdmin@123"}, auth=False),
    req("Lock User", "POST", "/auth/users/{{userId}}/lock"),
    req("Unlock User", "POST", "/auth/users/{{userId}}/unlock"),
]

# ---------------- Users ----------------
user_items = [
    req("Create User", "POST", "/users",
        body={"username": "jdoe", "email": "jdoe@hrms.local", "password": "Passw0rd!123",
              "fullName": "Jane Doe", "roleNames": ["EMPLOYEE"], "employeeId": 1}),
    req("Assign Roles", "PUT", "/users/{{userId}}/roles", body={"roleNames": ["EMPLOYEE", "MANAGER"]}),
    req("Activate User", "POST", "/users/{{userId}}/activate"),
    req("Deactivate User", "POST", "/users/{{userId}}/deactivate"),
    req("Get User By Id", "GET", "/users/{{userId}}"),
    req("List Users", "GET", "/users", params={"page": 0, "size": 20}),
]

# ---------------- Employees ----------------
employee_body = {
    "firstName": "Jane", "lastName": "Doe", "cnic": "12345-6789012-3",
    "dateOfBirth": "1995-04-12", "gender": "FEMALE", "maritalStatus": "SINGLE",
    "bloodGroup": "O+", "mobileNumber": "+923001234567", "email": "jane.doe@hrms.local",
    "emergencyContact": "+923007654321", "address": "123 Main St, Lahore",
    "joiningDate": "2024-01-15", "departmentId": 1, "designationId": 1,
    "postingUnitId": 1, "salary": 100000, "bonus": 0, "medicalAllocation": 50000,
    "employmentStatus": "ACTIVE",
}
employee_items = [
    req("Create Employee", "POST", "/employees", body=employee_body),
    req("Update Employee", "PUT", "/employees/{{employeeId}}", body=employee_body),
    req("Delete Employee", "DELETE", "/employees/{{employeeId}}"),
    req("Get Employee By Id", "GET", "/employees/{{employeeId}}"),
    req("Get Employee By Code", "GET", "/employees/code/EMP-2026-00001"),
    req("List/Filter Employees", "GET", "/employees",
        params={"page": 0, "size": 20, "sort": "lastName,asc", "departmentId": 1, "active": "true"}),
    req("Search Employees", "GET", "/employees/search", params={"query": "jane", "page": 0, "size": 20}),
    req("Employee Directory", "GET", "/employees/directory", params={"page": 0, "size": 50}),
]

# ---------------- Master Data ----------------
master_data_items = [
    req("Create Department", "POST", "/master-data/departments", body={"name": "Engineering"}),
    req("Update Department", "PUT", "/master-data/departments/{{departmentId}}", body={"name": "Engineering & QA"}),
    req("Delete Department", "DELETE", "/master-data/departments/{{departmentId}}"),
    req("List Departments", "GET", "/master-data/departments"),
    req("Create Designation", "POST", "/master-data/designations", body={"title": "Software Engineer"}),
    req("Update Designation", "PUT", "/master-data/designations/{{designationId}}", body={"title": "Senior Software Engineer"}),
    req("Delete Designation", "DELETE", "/master-data/designations/{{designationId}}"),
    req("List Designations", "GET", "/master-data/designations"),
    req("Create Posting Unit", "POST", "/master-data/posting-units",
        body={"name": "Karachi Factory", "unitType": "FACTORY", "branchName": None}),
    req("Update Posting Unit", "PUT", "/master-data/posting-units/{{postingUnitId}}",
        body={"name": "Karachi Factory (Site 2)", "unitType": "FACTORY", "branchName": None}),
    req("Delete Posting Unit", "DELETE", "/master-data/posting-units/{{postingUnitId}}"),
    req("List Posting Units", "GET", "/master-data/posting-units"),
]

# ---------------- Leave ----------------
leave_items = [
    req("Apply Leave", "POST", "/leaves",
        body={"leaveType": "ANNUAL_LEAVE", "startDate": "2026-08-10", "endDate": "2026-08-12",
              "reason": "Family trip"}),
    req("Cancel Leave", "POST", "/leaves/{{leaveRequestId}}/cancel"),
    req("Approve Leave", "POST", "/leaves/{{leaveRequestId}}/approve", body={"remarks": "Approved, enjoy"}),
    req("Reject Leave", "POST", "/leaves/{{leaveRequestId}}/reject", body={"remarks": "Insufficient coverage"}),
    req("My Leave History", "GET", "/leaves/my-history", params={"page": 0, "size": 20}),
    req("Employee Leave History", "GET", "/leaves/employee/{{employeeId}}/history", params={"page": 0, "size": 20}),
    req("Pending Approvals (Manager)", "GET", "/leaves/pending-approvals", params={"page": 0, "size": 20}),
    req("My Leave Balance", "GET", "/leaves/my-balance", params={"year": 2026}),
    req("Employee Leave Balance", "GET", "/leaves/employee/{{employeeId}}/balance", params={"year": 2026}),
]

leave_encashment_items = [
    req("Request Encashment", "POST", "/leaves/encashment", body={"leaveType": "ANNUAL_LEAVE", "days": 5}),
    req("Approve Encashment", "POST", "/leaves/encashment/{{encashmentId}}/approve", body={"remarks": "Approved"}),
    req("Reject Encashment", "POST", "/leaves/encashment/{{encashmentId}}/reject", body={"remarks": "Policy limit exceeded"}),
    req("My Encashment History", "GET", "/leaves/encashment/my-history", params={"page": 0, "size": 20}),
]

# ---------------- Medical Claims ----------------
medical_items = [
    req("Create Claim (multipart)", "POST", "/medical-claims",
        form_data=[
            {"key": "claim", "type": "text",
             "value": json.dumps({"description": "Dental checkup", "amount": 1500.00, "claimDate": "2026-06-01"})},
            {"key": "attachment", "type": "file"},
        ]),
    req("Upload/Replace Attachment (multipart)", "POST", "/medical-claims/{{claimId}}/attachment",
        form_data=[{"key": "attachment", "type": "file"}]),
    req("HR Approve Claim", "POST", "/medical-claims/{{claimId}}/hr-approve", body={"remarks": "Looks valid"}),
    req("HR Reject Claim", "POST", "/medical-claims/{{claimId}}/hr-reject", body={"remarks": "Missing receipt"}),
    req("Finance Approve Claim", "POST", "/medical-claims/{{claimId}}/finance-approve", body={"remarks": "Within budget"}),
    req("Finance Reject Claim", "POST", "/medical-claims/{{claimId}}/finance-reject", body={"remarks": "Exceeds annual budget"}),
    req("Get Claim By Id", "GET", "/medical-claims/{{claimId}}"),
    req("My Claim History", "GET", "/medical-claims/my-history", params={"page": 0, "size": 20}),
    req("Employee Claim History", "GET", "/medical-claims/employee/{{employeeId}}/history", params={"page": 0, "size": 20}),
    req("Pending HR Review", "GET", "/medical-claims/pending-hr", params={"page": 0, "size": 20}),
    req("Pending Finance Review", "GET", "/medical-claims/pending-finance", params={"page": 0, "size": 20}),
    req("My Medical Budget", "GET", "/medical-claims/budget/my", params={"year": 2026}),
    req("Employee Medical Budget", "GET", "/medical-claims/budget/employee/{{employeeId}}", params={"year": 2026}),
]

# ---------------- Provident Fund ----------------
pf_items = [
    req("Open PF Account", "POST", "/pf/accounts/{{employeeId}}",
        body={"employeeContributionRate": 10, "employerContributionRate": 10, "openingBalance": 0}),
    req("My PF Account", "GET", "/pf/accounts/my"),
    req("Employee PF Account", "GET", "/pf/accounts/employee/{{employeeId}}"),
    req("Request PF Withdrawal", "POST", "/pf/withdrawals", body={"amount": 5000, "reason": "Medical emergency"}),
    req("Approve PF Withdrawal", "POST", "/pf/withdrawals/{{withdrawalId}}/approve", body={"remarks": "Approved"}),
    req("Reject PF Withdrawal", "POST", "/pf/withdrawals/{{withdrawalId}}/reject", body={"remarks": "Insufficient justification"}),
    req("My PF Withdrawal History", "GET", "/pf/withdrawals/my-history", params={"page": 0, "size": 20}),
    req("Pending PF Withdrawals", "GET", "/pf/withdrawals/pending", params={"page": 0, "size": 20}),
    req("My PF Statement", "GET", "/pf/statement/my", params={"page": 0, "size": 20}),
    req("Employee PF Statement", "GET", "/pf/statement/employee/{{employeeId}}", params={"page": 0, "size": 20}),
    req("Org-wide PF Ledger", "GET", "/pf/ledger", params={"page": 0, "size": 50}),
]

# ---------------- Postings ----------------
posting_items = [
    req("Transfer Employee", "POST", "/postings/employees/{{employeeId}}/transfer",
        body={"newPostingUnitId": 2, "newDepartmentId": None, "transferDate": "2026-07-01",
              "reason": "Business requirement"}),
    req("Employee Posting History", "GET", "/postings/employees/{{employeeId}}/history", params={"page": 0, "size": 20}),
]

# ---------------- Roles & Permissions ----------------
role_items = [
    req("Create Role", "POST", "/roles", body={"name": "TEAM_LEAD", "description": "Leads a small team"}),
    req("Update Role", "PUT", "/roles/{{roleId}}", body={"name": "TEAM_LEAD", "description": "Updated description"}),
    req("Delete Role", "DELETE", "/roles/{{roleId}}"),
    req("Get Role By Id", "GET", "/roles/{{roleId}}"),
    req("List All Roles", "GET", "/roles"),
    req("Assign Permissions To Role", "PUT", "/roles/{{roleId}}/permissions", body={"permissionIds": [1, 2, 3]}),
    req("Assign Users To Role", "POST", "/roles/{{roleId}}/users", body={"userIds": [2, 3]}),
    req("Remove User From Role", "DELETE", "/roles/{{roleId}}/users/{{userId}}"),
    req("Get Users With Role", "GET", "/roles/MANAGER/users"),
]

permission_items = [
    req("List Permissions", "GET", "/permissions"),
    req("Create Permission", "POST", "/permissions",
        body={"name": "REPORT_EXPORT", "description": "Export dashboard reports", "module": "DASHBOARD"}),
]

# ---------------- Dashboard ----------------
dashboard_items = [
    req("Employee Summary", "GET", "/dashboard/employees/summary"),
    req("Employees By Department", "GET", "/dashboard/employees/by-department"),
    req("Employees By Gender", "GET", "/dashboard/employees/by-gender"),
    req("Employees By Branch", "GET", "/dashboard/employees/by-branch"),
    req("Employees By Status", "GET", "/dashboard/employees/by-status"),
    req("Monthly Joining Trend", "GET", "/dashboard/employees/joining-trend", params={"year": 2026}),
    req("Monthly Resignation Trend", "GET", "/dashboard/employees/resignation-trend", params={"year": 2026}),
    req("Leave Statistics", "GET", "/dashboard/leave/statistics", params={"year": 2026}),
    req("Medical Statistics", "GET", "/dashboard/medical/statistics", params={"year": 2026}),
    req("PF Statistics", "GET", "/dashboard/pf/statistics"),
]

collection = {
    "info": {
        "name": "HRMS Backend API",
        "description": "Full endpoint catalog for the HRMS backend (Phases 1-6: Auth, Employees, Leave, Medical Claims, PF, Postings, Roles, Dashboard). Run 'Login' first - it auto-populates {{accessToken}}/{{refreshToken}} for every other request.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    },
    "item": [
        folder("Auth", auth_items),
        folder("Users", user_items),
        folder("Employees", employee_items),
        folder("Master Data", master_data_items),
        folder("Leave", leave_items),
        folder("Leave Encashment", leave_encashment_items),
        folder("Medical Claims", medical_items),
        folder("Provident Fund", pf_items),
        folder("Postings", posting_items),
        folder("Roles", role_items),
        folder("Permissions", permission_items),
        folder("Dashboard", dashboard_items),
    ],
    "variable": [
        {"key": "baseUrl", "value": "http://localhost:8080", "type": "string"},
        {"key": "accessToken", "value": "", "type": "string"},
        {"key": "refreshToken", "value": "", "type": "string"},
        {"key": "userId", "value": "1", "type": "string"},
        {"key": "employeeId", "value": "1", "type": "string"},
        {"key": "departmentId", "value": "1", "type": "string"},
        {"key": "designationId", "value": "1", "type": "string"},
        {"key": "postingUnitId", "value": "1", "type": "string"},
        {"key": "leaveRequestId", "value": "1", "type": "string"},
        {"key": "encashmentId", "value": "1", "type": "string"},
        {"key": "claimId", "value": "1", "type": "string"},
        {"key": "withdrawalId", "value": "1", "type": "string"},
        {"key": "roleId", "value": "1", "type": "string"},
        {"key": "passwordResetToken", "value": "", "type": "string"},
    ],
}

with open("hrms-backend.postman_collection.json", "w") as f:
    json.dump(collection, f, indent=2)

total_requests = sum(len(f["item"]) for f in collection["item"])
print(f"Generated collection with {len(collection['item'])} folders and {total_requests} requests")
