# Script to save content of .java, .json and .xml files to a text file
param(
    [string]$SourceDirectory = ".",
    [string]$OutputFile = "all_files_content.txt",
    [string]$Encoding = "UTF8"
)

$SourceFullPath = (Get-Item $SourceDirectory).FullName
Write-Host "Searching files in: $SourceFullPath" -ForegroundColor Green

# Find files
$files = Get-ChildItem -Path $SourceDirectory -Recurse -Include "*.java", "*.json", "*.xml" | 
         Where-Object { $_.PSIsContainer -eq $false }

Write-Host "Found files: $($files.Count)" -ForegroundColor Yellow

# Statistics by file type
$javaFiles = $files | Where-Object { $_.Extension -eq ".java" }
$jsonFiles = $files | Where-Object { $_.Extension -eq ".json" }
$xmlFiles = $files | Where-Object { $_.Extension -eq ".xml" }

Write-Host "File distribution:" -ForegroundColor Cyan
Write-Host "  .java: $($javaFiles.Count)" -ForegroundColor White
Write-Host "  .json: $($jsonFiles.Count)" -ForegroundColor White
Write-Host "  .xml: $($xmlFiles.Count)" -ForegroundColor White

# Create file header
$header = @"
===================================================
PROJECT FILES CONTENT
Project: $(Split-Path $SourceFullPath -Leaf)
Created: $(Get-Date)
Total files: $($files.Count)
===================================================

"@

$header | Out-File -FilePath $OutputFile -Encoding $Encoding

# Process files
$successCount = 0
$errorCount = 0
$totalSize = 0

foreach ($file in $files) {
    try {
        $relativePath = $file.FullName.Replace($SourceFullPath, "").TrimStart('\', '/')
        $fileSize = $file.Length
        $totalSize += $fileSize
        
        # Add file separator with info
        $fileHeader = @"

===================================================
FILE: $relativePath
Size: $fileSize bytes
Modified: $($file.LastWriteTime)
===================================================

"@
        $fileHeader | Out-File -FilePath $OutputFile -Encoding $Encoding -Append
        
        # Read and add file content
        $content = Get-Content -Path $file.FullName -Encoding UTF8 -Raw
        $content | Out-File -FilePath $OutputFile -Encoding $Encoding -Append
        
        Write-Host "  OK Processed: $relativePath" -ForegroundColor Green
        $successCount++
    }
    catch {
        $errorMsg = "  ERROR: $relativePath - $($_.Exception.Message)"
        $errorMsg | Out-File -FilePath $OutputFile -Encoding $Encoding -Append
        Write-Host $errorMsg -ForegroundColor Red
        $errorCount++
    }
}

# Add final statistics
$footer = @"

===================================================
SUMMARY:
Successfully processed: $successCount
Errors: $errorCount
Total size: $totalSize bytes ($([math]::Round($totalSize/1KB, 2)) KB)
Completed: $(Get-Date)
===================================================
"@

$footer | Out-File -FilePath $OutputFile -Encoding $Encoding -Append

# Show results
Write-Host "`n" + "=" * 50 -ForegroundColor Cyan
Write-Host "PROCESSING COMPLETED!" -ForegroundColor Yellow
Write-Host "Success: $successCount" -ForegroundColor Green
Write-Host "Errors: $errorCount" -ForegroundColor Red
Write-Host "Total size: $([math]::Round($totalSize/1KB, 2)) KB" -ForegroundColor Cyan
Write-Host "Result saved to: $OutputFile" -ForegroundColor Green

# Check created file
$resultFile = Get-Item $OutputFile -ErrorAction SilentlyContinue
if ($resultFile) {
    Write-Host "Output file size: $([math]::Round($resultFile.Length/1MB, 2)) MB" -ForegroundColor White
    
    # Show first 10 lines for verification
    Write-Host "`nFirst 10 lines of result:" -ForegroundColor Yellow
    Get-Content $OutputFile -Encoding $Encoding | Select-Object -First 10 | ForEach-Object { 
        Write-Host "  $_" -ForegroundColor Gray 
    }
} else {
    Write-Host "Error: output file was not created!" -ForegroundColor Red
}