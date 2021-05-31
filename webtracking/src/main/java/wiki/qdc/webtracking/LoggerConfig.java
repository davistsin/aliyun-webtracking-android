package wiki.qdc.webtracking;

public final class LoggerConfig {
    final boolean offlineMode;
    final int maxOfflineNum;
    final boolean enablePrint;

    LoggerConfig(LoggerConfig.Builder builder) {
        this.offlineMode = builder.offlineMode;
        this.maxOfflineNum = builder.maxOfflineNum;
        this.enablePrint = builder.enablePrint;
    }

    public static class Builder {
        private boolean offlineMode = false;
        private int maxOfflineNum = 50;
        private boolean enablePrint = false;

        /**
         * 设置离线模式。当因网络问题上传日志失败时，会添加到离线存储中。在下次执行 init() 方法时，上传全部离线的日志。
         * @param offlineMode boolean, default false
         * @return
         */
        public Builder setOfflineMode(boolean offlineMode) {
            this.offlineMode = offlineMode;
            return this;
        }

        /**
         * 设置离线模式最大可存储的日志条数。
         * @param maxOfflineNum int, default 50
         * @return
         */
        public Builder setMaxOfflineNum(int maxOfflineNum) {
            this.maxOfflineNum = maxOfflineNum;
            return this;
        }

        /**
         * 设置开启该库的调试日志。
         * @param enable boolean, default false
         * @return
         */
        public Builder enablePrint(boolean enable) {
            this.enablePrint = enable;
            return this;
        }

        public LoggerConfig build() {
            return new LoggerConfig(this);
        }
    }
}
