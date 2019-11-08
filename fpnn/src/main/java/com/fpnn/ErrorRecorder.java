package com.fpnn;

public class ErrorRecorder {

    public interface IErrorRecorder {
        void recordError(Exception ex);
    }

    private ErrorRecorder() {}

    private static class Singleton {
        private static final ErrorRecorder INSTANCE = new ErrorRecorder();
    }

    public static final ErrorRecorder getInstance() {
        return ErrorRecorder.Singleton.INSTANCE;
    }

    private IErrorRecorder _recorder = null;

    public synchronized void setRecorder (IErrorRecorder value) {
        this._recorder = value;
    }

    public IErrorRecorder getRecorder() {
        return this._recorder;
    }

    public void recordError(Exception ex) {
        if (this._recorder == null) {
            this.setRecorder(new IErrorRecorder() {
                @Override
                public void recordError(Exception ex) {
                    ex.printStackTrace();
                }
            });
        }

        this._recorder.recordError(ex);
    }
}
