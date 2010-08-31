#!/bin/bash


format()
{
	res=$1
	if [ $1 -lt 10 ]; then
		#return "00$1"
		res="00$1"
	elif [ $1 -lt 100 ]; then
		#return "0$1"
		res="0$1"
	fi
}

let p3=$1%1000

format $p3
p3=$res

echo "$p3"

