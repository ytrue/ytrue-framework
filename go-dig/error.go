package dig

import "fmt"

func newErrInvalidInput(msg string, cause error) errInvalidInput {
	return errInvalidInput{msg, cause}
}

type errInvalidInput struct {
	Message string
	Cause   error
}

func (e errInvalidInput) Error() string { return fmt.Sprint(e.Message) }
