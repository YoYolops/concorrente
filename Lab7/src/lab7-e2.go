package main

import (
	"math/rand"
	"fmt"
)

func random_generator(out chan int) {
	rand.Seed(42)
	for i:=0; i<10_000; i++ {
		v := rand.Intn(100)
		out <- v
	}
	close(out)
}

func main() {
	minVal := 50
	ch := make(chan int)
	go random_generator(ch)

	for number := range ch {
		if number > minVal {
			fmt.Println(number)
		}
	}
}