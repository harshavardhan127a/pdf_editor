$url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
$outZip = "d:\Random\WROK\Projects\PDF\.mvn\maven.zip"
$extractTo = "d:\Random\WROK\Projects\PDF\.mvn"

Write-Host "Downloading Maven 3.9.9..."
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Invoke-WebRequest -Uri $url -OutFile $outZip -UseBasicParsing

Write-Host "Extracting..."
Expand-Archive -Path $outZip -DestinationPath $extractTo -Force

Remove-Item $outZip -Force
Write-Host "Done! Maven available at: $extractTo\apache-maven-3.9.9\bin\mvn.cmd"
