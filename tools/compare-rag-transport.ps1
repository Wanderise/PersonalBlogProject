param(
    [Parameter(Mandatory = $true)][string]$Token,
    [Parameter(Mandatory = $true)][int]$KnowledgeBaseId,
    [Parameter(Mandatory = $true)][string]$FilesDirectory,
    [ValidateSet("local", "rabbitmq")][string]$Transport,
    [string]$ApiBase = "http://localhost:8080",
    [string]$Output = "rag-transport-results.csv"
)

$headers = @{ Authorization = "Bearer $Token" }
$results = @()
$files = Get-ChildItem -LiteralPath $FilesDirectory -File |
    Where-Object { $_.Extension -in ".pdf", ".doc", ".docx", ".txt" }

foreach ($file in $files) {
    $totalWatch = [System.Diagnostics.Stopwatch]::StartNew()
    $ackWatch = [System.Diagnostics.Stopwatch]::StartNew()
    $json = & curl.exe -sS -X POST "$ApiBase/ai/rag/upload" `
        -H "Authorization: Bearer $Token" `
        -F "files=@$($file.FullName)" `
        -F "knowledgeBaseId=$KnowledgeBaseId"
    if ($LASTEXITCODE -ne 0) { throw "Upload failed for $($file.Name)" }
    $response = $json | ConvertFrom-Json
    $ackWatch.Stop()

    $document = @($response.data)[0]
    $status = $document.status
    while ($status -eq "PROCESSING" -and $totalWatch.Elapsed.TotalMinutes -lt 5) {
        Start-Sleep -Milliseconds 500
        $list = Invoke-RestMethod -Method Get `
            -Uri "$ApiBase/ai/knowledge-bases/$KnowledgeBaseId/documents" `
            -Headers $headers
        $status = (@($list.data) | Where-Object id -eq $document.id).status
    }
    $totalWatch.Stop()
    $results += [pscustomobject]@{
        transport = $Transport
        file = $file.Name
        bytes = $file.Length
        ack_ms = $ackWatch.ElapsedMilliseconds
        ready_ms = $totalWatch.ElapsedMilliseconds
        result = $status
    }
}

$results | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $Output
$results | Format-Table -AutoSize
