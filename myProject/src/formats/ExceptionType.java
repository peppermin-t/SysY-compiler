package formats;

public enum ExceptionType {
    ILLEGALCHAR, IDENTDOUBLED, IDENTUNDEFINED, PARAMSCNTUNMATCHED,
    PARAMSTYPEUNMATCHED, RETURNVARINVOIDFUNC, RETURNMISSINGINNONVOIDFUNC,
    CONSTVARALTERED, SEMICOLONMISSING, RPARENTMISSING,
    RBRACKMISSING, PRINTFEXPCNTUNMATCHED, BREAKCONTINUEINNONLOOP
}