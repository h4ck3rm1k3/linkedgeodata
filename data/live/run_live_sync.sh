#!/bin/bash

# TODO Move this function to a common file
m_error()
{
        echo "[`date +"%Y-%m-%d %H:%M:%S"`] $$ [error] $1"
        exit 1
}



while [ 1 ]
do
	if ! time ./singleDownload.sh; then
		m_error "An error occurred"
	fi
done

