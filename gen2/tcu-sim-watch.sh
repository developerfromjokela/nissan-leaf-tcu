#!/usr/bin/env bash
# Gen2 Ficosa TCU over USB: network registration + incoming SMS sniffing.
# See SIMSLOT-TROUBLESHOOTING.md §4 (AT ports) and §8 (DIAG).
set -uo pipefail

AT_PORT=${AT_PORT:-/dev/ttyUSB3}
DIAG_PORT=${DIAG_PORT:-/dev/ttyUSB0}
NMEA_PORT=${NMEA_PORT:-/dev/ttyUSB2}
INTERVAL=${INTERVAL:-5}
AT_TIMEOUT=${AT_TIMEOUT:-3}
QCSUPER_PYTHON=${QCSUPER_PYTHON:-uvx --from qcsuper python}

usage() {
  cat <<EOF
usage: $0 <command>
  net         poll AT+COPS? / AT+CGACT? every \$INTERVAL s, print on change   (\$AT_PORT)
  sms-diag    passive SMS sniff on DIAG via qcsuper + tshark                 (\$DIAG_PORT)
  debug       ATENABLEDEBUG, stream firmware log from NMEA port + tcu-debug-*.log (\$AT_PORT, \$NMEA_PORT)
  selftest    check the +COPS parser

env: AT_PORT=$AT_PORT DIAG_PORT=$DIAG_PORT NMEA_PORT=$NMEA_PORT INTERVAL=$INTERVAL
EOF
  exit 1
}

ts() { date '+%F %T'; }

open_port() {
  [[ -r $1 && -w $1 ]] || { echo "cannot open $1 (dialout group?)" >&2; exit 1; }
  stty -F "$1" 115200 raw -echo clocal 2>/dev/null
}

at() {
  local dev=$1 cmd=$2 line
  exec 3<>"$dev"
  printf '%s\r' "$cmd" >&3
  while IFS= read -r -t "$AT_TIMEOUT" line <&3; do
    line=${line%$'\r'}
    [[ -z $line || $line == "$cmd" ]] && continue
    echo "$line"
    [[ $line == OK || $line == *ERROR* ]] && break
  done
  exec 3>&-
}

access_tech() {
  case $1 in
    0) echo GSM ;; 2) echo UTRAN ;; 3) echo GSM-EGPRS ;; 4|5|6) echo HSPA ;; 7) echo LTE ;;
    "") echo - ;; *) echo "act=$1" ;;
  esac
}

parse_cops() {
  local re='\+COPS: [0-9]+,[0-9]+,"([^"]*)"(,([0-9]+))?'
  if [[ $1 =~ $re ]] && [[ ${BASH_REMATCH[1]} != "Limited Service" ]]; then
    echo "REGISTERED ${BASH_REMATCH[1]} $(access_tech "${BASH_REMATCH[3]}")"
  elif [[ $1 == *"Limited Service"* ]]; then
    echo "LIMITED (camped, not registered)"
  else
    echo "NO SERVICE"
  fi
}

parse_cgact() {
  local active
  active=$(grep -oE '\+CGACT: [0-9]+,1' <<<"$1" | cut -d' ' -f2 | cut -d, -f1 | paste -sd, -)
  [[ -n $active ]] && echo "data UP (cid $active)" || echo "no active bearer (TCU connects on demand)"
}

cmd_net() {
  open_port "$AT_PORT"
  local last="" state
  while :; do
    state="$(parse_cops "$(at "$AT_PORT" 'AT+COPS?')") | $(parse_cgact "$(at "$AT_PORT" 'AT+CGACT?')")"
    [[ $state != "$last" ]] && echo "$(ts) $state"
    last=$state
    sleep "$INTERVAL"
  done
}

UNBUFFERED_QCSUPER='
import builtins, sys
real_open = builtins.open
def unbuffered_binary_writes(f, mode="r", *a, **k):
    if "b" in mode and ("w" in mode or "a" in mode):
        return real_open(f, mode, 0)
    return real_open(f, mode, *a, **k)
builtins.open = unbuffered_binary_writes
sys.argv[0] = "qcsuper"
from qcsuper.main import main
sys.exit(main())
'

cmd_sms_diag() {
  command -v "${QCSUPER_PYTHON%% *}" >/dev/null || { echo "install uv, or set QCSUPER_PYTHON to a python with qcsuper" >&2; exit 1; }
  command -v tshark >/dev/null || { echo "install tshark" >&2; exit 1; }
  pgrep -x ModemManager >/dev/null && { echo "stop ModemManager first: sudo systemctl stop ModemManager" >&2; exit 1; }
  local pcap="diag-$(date +%Y%m%d-%H%M%S).pcap"
  $QCSUPER_PYTHON -c "$UNBUFFERED_QCSUPER" --usb-modem "$DIAG_PORT" --pcap-dump "$pcap" --decrypt-nas &
  local qpid=$!
  trap 'kill $qpid 2>/dev/null' EXIT INT TERM
  echo "$(ts) capturing to $pcap; SMS rows: time | from | dcs | text (binary SMS: open pcap in wireshark)"
  tail -c +1 -F "$pcap" 2>/dev/null |
    tshark -l -r - -Y gsm_sms -T fields -E separator=' | ' \
      -e frame.time -e gsm_sms.tp-oa -e gsm_sms.tp-dcs -e gsm_sms.sms_text
}

cmd_debug() {
  open_port "$AT_PORT"
  open_port "$NMEA_PORT"
  local log="tcu-debug-$(date +%Y%m%d-%H%M%S).log"
  at "$AT_PORT" ATDISABLEDEBUG >/dev/null
  until at "$AT_PORT" ATENABLEDEBUG | grep -qx OK; do
    echo "$(ts) ATENABLEDEBUG not accepted yet (TCU booting?), retrying" >&2
    sleep 2
  done
  echo "> ATENABLEDEBUG: OK, logging to $log"
  tr -d '\r' <"$NMEA_PORT" | grep --line-buffered -v '^\$' | tee "$log"
}

cmd_selftest() {
  [[ $(parse_cops '+COPS: 0,0,"One HU",7') == "REGISTERED One HU LTE" ]] &&
  [[ $(parse_cops '+COPS: 0,0,"Limited Service",0') == LIMITED* ]] &&
  [[ $(parse_cops '+COPS: 0') == "NO SERVICE" ]] &&
  [[ $(parse_cgact $'+CGACT: 1,1\n+CGACT: 2,0\n+CGACT: 3,1\nOK') == "data UP (cid 1,3)" ]] &&
  [[ $(parse_cgact 'OK') == no\ active* ]] &&
  echo ok || { echo FAIL; exit 1; }
}

case ${1:-} in
  net) cmd_net ;;
  sms-diag) cmd_sms_diag ;;
  debug) cmd_debug ;;
  selftest) cmd_selftest ;;
  *) usage ;;
esac
