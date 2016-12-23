set -x
sqlplus tm_cz/tm_cz@tm338 << EOF
exec i2b2_move_analysis_to_prod;
exit
EOF

