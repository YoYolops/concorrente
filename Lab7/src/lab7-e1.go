package main

import (
	"math/rand"
	"fmt"
)

func random_generator(out chan int) {
	rand.Seed(42)
	for {
		v := rand.Intn(100)
		out <- v
	}
}

// A main é a consumidora :)
func main() {
	minVal := 50
	ch := make(chan int)
	go random_generator(ch)

	for {
		number := <-ch
		if number > minVal {
			fmt.Println(number)
		}
	}
}