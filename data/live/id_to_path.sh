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

let p1=$1/1000000
let p2=($1%1000000)/1000
let p3=$1%1000

format $p1
p1=$res
format $p2
p2=$res
format $p3
p3=$res
#x=`format $p1`
#y=`format $p2`
#z=`format $p3`

echo "$p1/$p2"

