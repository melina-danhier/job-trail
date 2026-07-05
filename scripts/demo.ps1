param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$runId = "{0}-{1}" -f (Get-Date -Format "yyyyMMddHHmmss"), ([guid]::NewGuid().ToString("N").Substring(0, 8))
$email = "demo-$runId@example.invalid"
$password = "local-demo-123"

function Invoke-JsonRequest {
    param(
        [Parameter(Mandatory)] [string]$Method,
        [Parameter(Mandatory)] [string]$Uri,
        [object]$Body,
        [hashtable]$Headers = @{}
    )

    $parameters = @{
        Method = $Method
        Uri = "$BaseUrl$Uri"
        Headers = $Headers
    }
    if ($null -ne $Body) {
        $parameters.ContentType = "application/json"
        $parameters.Body = $Body | ConvertTo-Json -Depth 10
    }
    Invoke-RestMethod @parameters
}

function Assert-Demo {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) {
        throw "Demo assertion failed: $Message"
    }
}

Write-Host "1/9 Register $email"
$registered = Invoke-JsonRequest Post "/api/auth/register" @{
    email = $email
    password = $password
}
Assert-Demo ($registered.email -eq $email) "registered user differs"

Write-Host "2/9 Authenticate"
$token = Invoke-JsonRequest Post "/api/auth/login" @{
    email = $email
    password = $password
}
Assert-Demo (-not [string]::IsNullOrWhiteSpace($token)) "login returned no token"
$auth = @{ Authorization = "Bearer $token" }

Write-Host "3/9 Create company and application"
$company = Invoke-JsonRequest Post "/api/companies" @{
    name = "Demo Company $runId"
    website = "https://example.com"
    location = "Berlin"
} $auth
$application = Invoke-JsonRequest Post "/api/applications" @{
    positionTitle = "Backend Developer"
    companyId = $company.id
    applicationDate = (Get-Date -Format "yyyy-MM-dd")
    jobUrl = "https://example.com/jobs/backend"
} $auth
Assert-Demo ($application.status -eq "SAVED") "new application is not SAVED"

Write-Host "4/9 List applications"
$list = Invoke-JsonRequest Get "/api/applications?page=0&size=20" $null $auth
Assert-Demo ($list.content.id -contains $application.id) "application missing from list"

Write-Host "5/9 Filter applications"
$filtered = Invoke-JsonRequest Get "/api/applications?status=SAVED&companyId=$($company.id)" $null $auth
Assert-Demo ($filtered.content.id -contains $application.id) "application missing from filtered list"

Write-Host "6/9 Update application"
$updated = Invoke-JsonRequest Put "/api/applications/$($application.id)" @{
    positionTitle = "Senior Backend Developer"
    companyId = $company.id
    status = "SAVED"
    applicationDate = (Get-Date -Format "yyyy-MM-dd")
    jobUrl = "https://example.com/jobs/senior-backend"
} $auth
Assert-Demo ($updated.positionTitle -eq "Senior Backend Developer") "title was not updated"

Write-Host "7/9 Change status"
$statusChanged = Invoke-JsonRequest Patch "/api/applications/$($application.id)/status" @{
    status = "INTERVIEW_SCHEDULED"
} $auth
Assert-Demo ($statusChanged.status -eq "INTERVIEW_SCHEDULED") "status was not updated"

Write-Host "8/9 Read status history"
$history = @(Invoke-JsonRequest Get "/api/applications/$($application.id)/status-history" $null $auth)
Assert-Demo ($history.newStatus -contains "SAVED") "initial history entry is missing"
Assert-Demo ($history.newStatus -contains "INTERVIEW_SCHEDULED") "status history is incomplete"

Write-Host "9/9 Delete application and company"
Invoke-JsonRequest Delete "/api/applications/$($application.id)" $null $auth
Invoke-JsonRequest Delete "/api/companies/$($company.id)" $null $auth
$remaining = Invoke-JsonRequest Get "/api/applications" $null $auth
Assert-Demo ($remaining.content.id -notcontains $application.id) "deleted application is still listed"

Write-Host "Demo completed successfully." -ForegroundColor Green
