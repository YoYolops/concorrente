package main

import (
	"bufio"
	"encoding/gob"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"

	"github.com/fsnotify/fsnotify"
)

type Client struct {
	hashMap         map[int]string
	dataMutex       sync.RWMutex
	connectionMutex sync.Mutex
}

type HashChannelMessage struct {
    filePath string
    hash     int
    err      error
}

func NewClient() *Client {
	return &Client{
		hashMap:         make(map[int]string),
		dataMutex:       sync.RWMutex{},
		connectionMutex: sync.Mutex{},
	}
}

func readFile(filePath string) ([]byte, error) {
	data, err := os.ReadFile(filePath)
	if err != nil {
		fmt.Printf("Error reading file %s: %v", filePath, err)
		return nil, err
	}
	return data, nil
}

func sum(filePath string) (int, error) {
	data, err := readFile(filePath)
	if err != nil {
		return 0, err
	}

	_sum := 0
	for _, b := range data {
		_sum += int(b)
	}

	return _sum, nil
}

func listarArquivos(diretorio string) []string {
	arquivos, err := os.ReadDir(diretorio)
	if err != nil {
		log.Fatal(err)
	}

	var result []string
	for _, arquivo := range arquivos {
		if !arquivo.IsDir() {
			result = append(result, arquivo.Name())
		}
	}
	return result
}

// Versão SEM concorrência: sem goroutines e sem channels. (ADICIONAMOS A CONCORRÊNCIA)
func heavyDutyWorker(path string, sendChannel chan<- HashChannelMessage) {
	// trabalho pesado:
    val, err := sum(path)
    sendChannel <- HashChannelMessage{
        filePath: path,
        hash:     val,
        err:      err,
    }
}

func generateFilesHashMap(diretorio string) map[string][]int {
    if _, err := os.Stat(diretorio); os.IsNotExist(err) {
        log.Fatalf("O diretório %s não existe", diretorio)
    }

    files := listarArquivos(diretorio)
    totalFiles := len(files)

    messageChannel := make(chan HashChannelMessage, totalFiles)
    // uma função concorrente para cada arquivo que encontrei na pasta alvo
    for _, name := range files {
        fp := filepath.Join(diretorio, name)
        go heavyDutyWorker(fp, messageChannel)
    }

	hashs := make(map[string][]int, totalFiles)
    for i := 0; i < totalFiles; i++ {
        result := <-messageChannel
        if result.err != nil {
            log.Printf("Erro ao processar %s: %v", result.filePath, result.err)
            result.hash = 0
        }

        hashs[result.filePath] = append(hashs[result.filePath], result.hash)
    }

    // Como eu (geberateFilesHashMap) sei exatamente a quantidade de mensagens que receberei,
	// eu mesmo fecho o canal quando receber todas. Não preciso que as goroutines
	// façam isso para mim
    close(messageChannel)
    return hashs
}

func storeHashes(conn net.Conn, hashes map[string][]int, client *Client) {
	client.dataMutex.Lock()
    for path, hashList := range hashes {
        for _, h := range hashList {
            client.hashMap[h] = path
        }
    }
    client.dataMutex.Unlock()

	encoder := gob.NewEncoder(conn)

	if err := encoder.Encode("store"); err != nil {
		log.Println("Error encoding request type:", err)
		return
	}

	var hashList []int
	for _, v := range hashes {
		hashList = append(hashList, v...)
	}

	if err := encoder.Encode(hashList); err != nil {
		log.Println("Error encoding hashes:", err)
		return
	}

	fmt.Println("Initial hashes stored.")
}

func updateServer(conn net.Conn, action string, filePath string, client *Client) {
	client.connectionMutex.Lock()
	defer client.connectionMutex.Unlock()

	encoder := gob.NewEncoder(conn)

	if err := encoder.Encode(action); err != nil {
		log.Println("Error encoding action:", err)
		return
	}

	fileHash, err := sum(filePath)
	if err != nil {
		log.Printf("Error calculating hash for file %s: %v", filePath, err)
		fileHash = 0
	}

	if err := encoder.Encode(fileHash); err != nil {
		log.Println("Error encoding file hash:", err)
		return
	}

	// SEM mutex (intencional para os alunos implementarem controle depois)
	// Implementamos! :)
	client.dataMutex.Lock()
	client.hashMap[fileHash] = filePath
	client.dataMutex.Unlock()

	fmt.Printf("Server updated: %s - %s\n", action, filePath)
}

func monitorDirectory(conn net.Conn, directory string, server *Client) {
	watcher, err := fsnotify.NewWatcher()
	if err != nil {
		log.Fatal(err)
	}
	defer watcher.Close()

	err = watcher.Add(directory)
	if err != nil {
		log.Fatal(err)
	}

	for {
		select {
		case event, ok := <-watcher.Events:
			if !ok {
				return
			}
			if event.Op&fsnotify.Create == fsnotify.Create {
				fmt.Println("File created:", event.Name)
				updateServer(conn, "create", event.Name, server)
			}
			if event.Op&fsnotify.Remove == fsnotify.Remove {
				fmt.Println("File deleted:", event.Name)
				updateServer(conn, "delete", event.Name, server)
			}
		case err, ok := <-watcher.Errors:
			if !ok {
				return
			}
			fmt.Println("Error:", err)
		}
	}
}

// Alteramos para receber client
func queryHash(conn net.Conn, hash int, client *Client) ([]string, error) {
	client.connectionMutex.Lock()
	defer client.connectionMutex.Unlock()

	encoder := gob.NewEncoder(conn)
	if err := encoder.Encode("query"); err != nil {
		log.Println("Error encoding request type:", err)
		return nil, err
	}
	if err := encoder.Encode(hash); err != nil {
		log.Println("Error encoding hash:", err)
		return nil, err
	}
	decoder := gob.NewDecoder(conn)
	var ips []string
	if err := decoder.Decode(&ips); err == nil {
		fmt.Println("IPs for hash", hash, ":", ips)
	} else {
		log.Println("Error decoding result:", err)
	}

	return ips, nil
}

func (s *Client) handleDownloadRequest(conn net.Conn, decoder *gob.Decoder) {
	var fileHash int
	if err := decoder.Decode(&fileHash); err != nil {
		fmt.Println(err)
		log.Println("Error decoding file hash:", err)
		return
	}

	// SEM mutex (intencional)
	// implementamos :)
	s.dataMutex.RLock()
	filePath := s.hashMap[fileHash]
	s.dataMutex.RUnlock()

	file, err := os.Open("./" + filePath)
	if err != nil {
		fmt.Println("./" + filePath)
		fmt.Println(err)
		log.Println("Error opening file:", err)
		return
	}
	defer file.Close()

	chunkData, err := io.ReadAll(file)
	if err != nil {
		fmt.Print(err)
		log.Println("Error reading chunk data:", err)
		return
	}

	encoder := gob.NewEncoder(conn)
	if err := encoder.Encode(chunkData); err != nil {
		fmt.Print(err)
		log.Println("Error encoding chunk data:", err)
		return
	}

	log.Printf("Chunk with hash %d sent successfully\n", fileHash)
}

func (s *Client) handleConnection(conn net.Conn) {
	defer conn.Close()

	for {
		var requestType string
		decoder := gob.NewDecoder(conn)
		if err := decoder.Decode(&requestType); err != nil {
			if err.Error() == "EOF" {
				log.Println("Client disconnected")
				return
			}
			log.Println("Error decoding request type:", err)
			return
		}

		switch requestType {
		case "download":
			s.handleDownloadRequest(conn, decoder)
		default:
			log.Println("Unknown request type:", requestType)
		}
	}
}

func startClientServer(server *Client) {
	ln, err := net.Listen("tcp", ":9090")
	if err != nil {
		log.Fatal(err)
	}
	defer ln.Close()

	fmt.Println("Server is listening on port 8089...")

	for {
		conn, err := ln.Accept()
		if err != nil {
			log.Println("Error accepting connection:", err)
			continue
		}
		go server.handleConnection(conn)
	}
}

func donwload(hash int, ip string, outputPath string) error {
	conn, err := net.Dial("tcp", ip+":9090")
	if err != nil {
		fmt.Print("erroratrtrgfd")
		return fmt.Errorf("error connecting to server: %v", err)
	}
	defer conn.Close()

	encoder := gob.NewEncoder(conn)
	requestType := "download"
	if err := encoder.Encode(&requestType); err != nil {
		return fmt.Errorf("error sending request type: %v", err)
	}
	if err := encoder.Encode(&hash); err != nil {
		return fmt.Errorf("error sending file hash: %v", err)
	}

	decoder := gob.NewDecoder(conn)
	var chunkData []byte
	if err := decoder.Decode(&chunkData); err != nil {
		fmt.Printf("error receiving chunk data: %v", err)
		return fmt.Errorf("error receiving chunk data: %v", err)
	}

	if err := os.WriteFile(outputPath, chunkData, 0644); err != nil {
		return fmt.Errorf("error saving chunk to file: %v", err)
	}
	fmt.Printf("Chunk downloaded and saved to %s\n", outputPath)
	return nil
}

func main() {
	reader := bufio.NewReader(os.Stdin)
	fmt.Print("Enter server IP: ")
	serverIp, _ := reader.ReadString('\n')
	serverIp = strings.TrimSpace(serverIp)

	conn, err := net.Dial("tcp", serverIp+":8089")
	if err != nil {
		log.Fatal(err)
	}
	defer conn.Close()

	server := NewClient()
	directory := "./dataset/"
	initialHashes := generateFilesHashMap(directory)
	storeHashes(conn, initialHashes, server)

	go monitorDirectory(conn, directory, server)
	go startClientServer(server)

	for {
		fmt.Println("\nChoose an option:")
		fmt.Println("1. Query hash")
		fmt.Println("2. Download file")
		fmt.Println("3. Exit")
		fmt.Print("Enter choice (1, 2 or 3): ")

		choiceStr, err := reader.ReadString('\n')
		if err != nil {
			log.Fatal("Error reading input:", err)
		}
		choiceStr = strings.TrimSpace(choiceStr)
		choice, err := strconv.Atoi(choiceStr)
		if err != nil {
			log.Fatal("Invalid choice:", err)
		}

		switch choice {
		case 1:
			fmt.Print("Enter hash to query: ")
			hashInput, _ := reader.ReadString('\n')
			hashInput = strings.TrimSpace(hashInput)
			hash, err := strconv.Atoi(hashInput)
			if err != nil {
				log.Fatal("Invalid hash value:", err)
			}
			queryHash(conn, hash, server)

		case 2:
			fmt.Print("Enter hash to query: ")
			hashInput, _ := reader.ReadString('\n')
			hashInput = strings.TrimSpace(hashInput)
			hash, err := strconv.Atoi(hashInput)
			if err != nil {
				log.Fatal("Invalid hash value:", err)
			}

			fmt.Print("Enter file path to output: ")
			filePath, _ := reader.ReadString('\n')
			filePath = strings.TrimSpace(filePath)

			ips, err := queryHash(conn, hash, server)
			if err != nil {
				log.Fatal("Error while searching for IPs for the provided hash", err)
				continue
			}

			if len(ips) == 0 {
				fmt.Println("No IPs found for the provided hash.")
				continue
			}

			donwload(hash, strings.Split(ips[0], ":")[0], filePath)

		case 3:
			fmt.Println("Exiting...")
			return

		default:
			fmt.Println("Invalid choice. Please enter 1, 2 or 3.")
		}
	}
}
