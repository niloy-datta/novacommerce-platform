param(
    [string]$OutputDirectory = ".local/keys"
)

$openssl = Get-Command openssl -ErrorAction SilentlyContinue
if (-not $openssl) {
    throw "OpenSSL is required to generate local RSA keys. Install OpenSSL and run this script again."
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$privateKey = Join-Path $OutputDirectory "auth-private.pem"
$publicKey = Join-Path $OutputDirectory "auth-public.pem"

& $openssl.Source genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out $privateKey
& $openssl.Source rsa -pubout -in $privateKey -out $publicKey

Write-Output "Created local development keys in $OutputDirectory. These files are gitignored."
