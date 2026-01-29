package main

import (
	"fmt"
	"log"
)

var mem [1_000_000]int

var mem_size int = 2

func fib_task(n int) {
	for mem[n-1] == 0 {
		mem[mem_size] = mem[mem_size-1] + mem[mem_size-2]
		mem_size++
	}
	fmt.Printf("val: %d\n", mem[mem_size-1])
}

func main() {
	mem[0] = 1
	mem[1] = 1
	var num int
	_, err := fmt.Scan(&num)
	if err != nil {
		log.Fatal(err)
	}

	fib_task(num)
}
