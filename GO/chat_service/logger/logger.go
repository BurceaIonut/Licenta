package logger

import (
	"log"
)

type ILogger interface {
	Info(args ...any)
	Warning(args ...any)
	Error(args ...any)
	Debug(args ...any)
}

type Logger struct {
	InternalLogger *log.Logger
	InfoPrefix     string
	WarningPrefix  string
	ErrorPrefix    string
	DebugEnabled   bool
	DebugPrefix    string
}

func NewLogger(l *log.Logger, infoPrefix string, warningPrefix string, errorPrefix string) *Logger {
	return &Logger{InternalLogger: l, InfoPrefix: infoPrefix, WarningPrefix: warningPrefix, ErrorPrefix: errorPrefix, DebugEnabled: false, DebugPrefix: "[DEBUG]"}
}

func NewDebugLogger(l *log.Logger, infoPrefix string, warningPrefix string, errorPrefix string, debugPrefix string) *Logger {
	return &Logger{InternalLogger: l, InfoPrefix: infoPrefix, WarningPrefix: warningPrefix, ErrorPrefix: errorPrefix, DebugEnabled: true, DebugPrefix: debugPrefix}
}

func (log *Logger) Info(args ...any) {
	log.InternalLogger.Println(log.InfoPrefix, args)
}

func (log *Logger) Warning(args ...any) {
	log.InternalLogger.Println(log.WarningPrefix, args)
}

func (log *Logger) Error(args ...any) {
	log.InternalLogger.Println(log.ErrorPrefix, args)
}

func (log *Logger) Debug(args ...any) {
	if log.DebugEnabled {
		log.InternalLogger.Println(log.DebugPrefix, args)
	}
}
