#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "run this script as root" >&2
  exit 1
fi

api_host="${API_HOST:-}"
certbot_email="${CERTBOT_EMAIL:-}"
if [[ ! "${api_host}" =~ ^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$ ]]; then
  echo "API_HOST must be a DNS hostname without a scheme or path" >&2
  exit 1
fi
if [ -z "${certbot_email}" ]; then
  echo "CERTBOT_EMAIL is required for the initial certificate request" >&2
  exit 1
fi
command -v nginx >/dev/null || { echo "nginx is not installed" >&2; exit 1; }
command -v certbot >/dev/null || { echo "certbot is not installed" >&2; exit 1; }

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
site_target="/etc/nginx/sites-available/jbnu-sugang-helper.conf"
site_link="/etc/nginx/sites-enabled/jbnu-sugang-helper.conf"
acme_root="/var/www/certbot"

install -d -o root -g root -m 0755 \
  /etc/nginx/conf.d \
  /etc/nginx/snippets \
  /etc/nginx/sites-available \
  /etc/nginx/sites-enabled \
  "${acme_root}"
install -o root -g root -m 0644 \
  "${repo_root}/nginx/conf.d/00-sugang-helper-rate-limit.conf" \
  /etc/nginx/conf.d/00-sugang-helper-rate-limit.conf
install -o root -g root -m 0644 \
  "${repo_root}/nginx/snippets/jbnu-sugang-helper-proxy.conf" \
  /etc/nginx/snippets/jbnu-sugang-helper-proxy.conf

bootstrap_config="$(mktemp /etc/nginx/sites-available/jbnu-sugang-helper.acme.XXXXXX)"
trap 'rm -f "${bootstrap_config}" "${final_config:-}"' EXIT
cat >"${bootstrap_config}" <<EOF
server {
    listen 80;
    listen [::]:80;
    server_name ${api_host};

    location ^~ /.well-known/acme-challenge/ {
        root ${acme_root};
        try_files \$uri =404;
    }

    location / {
        return 404;
    }
}
EOF
ln -sfn "${bootstrap_config}" "${site_link}"
nginx -t
if systemctl is-active --quiet nginx; then
  systemctl reload nginx
else
  systemctl start nginx
fi

certbot certonly \
  --webroot -w "${acme_root}" \
  --domain "${api_host}" \
  --email "${certbot_email}" \
  --agree-tos --non-interactive --no-eff-email --keep-until-expiring

final_config="$(mktemp /etc/nginx/sites-available/jbnu-sugang-helper.final.XXXXXX)"
API_HOST="${api_host}" "${repo_root}/scripts/render-nginx-config.sh" "${final_config}"
install -o root -g root -m 0644 "${final_config}" "${site_target}"
ln -sfn "${site_target}" "${site_link}"
nginx -t
systemctl reload nginx
echo "Nginx ACME bootstrap and TLS configuration completed for ${api_host}"
