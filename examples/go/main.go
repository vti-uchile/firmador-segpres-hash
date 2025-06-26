package main

import (
	"context"
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/base64"
	"encoding/pem"
	"errors"
	"flag"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/vti-uchile/firmador-segpres-hash/examples/go/lib/proto"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

var (
	address  = flag.String("address", "localhost:8080", "the address to connect")
	size     = flag.Int("size", 4*1024*1024, "the maximum message size (in bytes) the client can send and receive")
	threads  = flag.Int("threads", 1, "the number of threads to send and receive messages")
	total    = flag.Int("total", 1, "the total number of messages to send and receive")
	name     = flag.String("filename", "document.pdf", "the file name to sign")
	image    = flag.String("signature", "", "the signature image file to stamp (empty for invisible layout)")
	rut      = flag.String("rut", "", "the signer RUT")
	key      = flag.String("key", "public.pem", "the public RSA key")
	password = flag.String("password", "", "the signer password or one-time password")
	page     = flag.Int("page", 1, "the page number to stamp the signature")
	llx      = flag.Int("llx", 0, "the lower left X coordinate to stamp the signature")
	lly      = flag.Int("lly", 0, "the lower left Y coordinate to stamp the signature")
	urx      = flag.Int("urx", 0, "the upper right X coordinate to stamp the signature")
	ury      = flag.Int("ury", 0, "the upper right Y coordinate to stamp the signature")
	attended = flag.Bool("attended", false, "whether the sign is attended (remember to set this for one-time passwords)")
	timeout  = flag.Duration("timeout", time.Second*10, "the duration of the context timeout")
	output   = flag.String("output", "signed-document.pdf", "the signed output file")
)

type Message struct {
	i         int
	client    proto.SignerClient
	file      []byte
	signature []byte
	wg        *sync.WaitGroup
}

var queue = make(chan *Message)

func encrypt(publicKey, msg []byte) ([]byte, error) {
	block, _ := pem.Decode(publicKey)
	if block == nil {
		return nil, errors.New("unable to decode public key")
	}

	pub, err := x509.ParsePKIXPublicKey(block.Bytes)
	if err != nil {
		return nil, err
	}

	return rsa.EncryptPKCS1v15(rand.Reader, pub.(*rsa.PublicKey), msg)
}

func (m *Message) send() {
	defer m.wg.Done()

	ctx, cancel := context.WithTimeout(context.Background(), *timeout)
	defer cancel()

	reply, err := m.client.Send(ctx, &proto.SignRequest{
		Name:      *name,
		File:      m.file,
		Signature: m.signature,
		Rut:       *rut,
		Password:  *password,
		Page:      int32(*page),
		Llx:       int32(*llx), // X1
		Lly:       int32(*lly), // Y1
		Urx:       int32(*urx), // X2
		Ury:       int32(*ury), // Y2
		Attended:  *attended,
	})
	if err != nil {
		log.Printf("unable to send message %d: %v", m.i, err)

		return
	}

	log.Printf("success: %t", reply.GetSuccess())
	log.Printf("invalid password: %t", reply.GetInvalidPassword())
	log.Printf("retry: %t", reply.GetRetry())
	log.Printf("message: %s", reply.GetMessage())

	if reply.GetSuccess() {
		extension := filepath.Ext(*output)
		filename := strings.TrimSuffix(*output, extension)
		filename = fmt.Sprintf("%s-%d%s", filename, m.i, extension)

		if err := os.WriteFile(filename, reply.GetFile(), 0644); err != nil {
			log.Printf("unable to write signed file %s: %v", filename, err)
		}
	}
}

func dequeue() {
	if *threads < 1 {
		*threads = 1
	} else if *threads > 20 {
		*threads = 20
	}

	for range *threads {
		go func() {
			for message := range queue {
				message.send()
			}
		}()
	}
}

func getTotal() int {
	if *total < 1 {
		return 1
	} else if *total > 500 {
		return 500
	}

	return *total
}

func main() {
	flag.Parse()

	if *rut == "" {
		log.Fatal("got empty signer RUT")
	}

	conn, err := grpc.NewClient(
		*address,
		grpc.WithTransportCredentials(insecure.NewCredentials()),
		grpc.WithBlock(),
		grpc.WithTimeout(*timeout),
		grpc.WithDefaultCallOptions(
			grpc.MaxCallSendMsgSize(*size),
			grpc.MaxCallRecvMsgSize(*size),
		),
	)
	if err != nil {
		log.Fatalf("unable to connect to %s: %v", *address, err)
	}
	defer func() {
		_ = conn.Close()
	}()

	file, err := os.ReadFile(*name)
	if err != nil {
		log.Printf("unable to read file %s: %v", *name, err)

		return
	}

	signature := []byte("") // invisible

	if *image != "" {
		signature, err = os.ReadFile(*image)
		if err != nil {
			log.Printf("unable to read signature from file %s: %v", *image, err)

			return
		}
	}

	if *password != "" {
		publicKey, err := os.ReadFile(*key)
		if err != nil {
			log.Printf("unable to read public key from file %s: %v", *key, err)

			return
		}

		encrypted, err := encrypt(publicKey, []byte(*password))
		if err != nil {
			log.Printf("unable to encrypt password: %v", err)

			return
		}

		*password = base64.StdEncoding.EncodeToString(encrypted)
	}

	dequeue()

	client := proto.NewSignerClient(conn)

	wg := sync.WaitGroup{}

	for i := 1; i <= getTotal(); i++ {
		wg.Add(1)

		queue <- &Message{i, client, file, signature, &wg}
	}

	wg.Wait()
}
