use thiserror::Error;

pub type Result<T> = std::result::Result<T, Error>;

/// Closed set of domain errors. Platform layers translate these to user copy.
#[derive(Debug, Error)]
pub enum Error {
    #[error("invalid URI: {0}")]
    InvalidUri(String),

    #[error("unsupported protocol: {0}")]
    UnsupportedProtocol(String),

    #[error("missing required field: {0}")]
    MissingField(&'static str),

    #[error("invalid value for {field}: {reason}")]
    InvalidValue { field: &'static str, reason: String },

    #[error("decode error: {0}")]
    Decode(String),

    #[error("serialization error: {0}")]
    Serialization(String),
}

impl From<serde_json::Error> for Error {
    fn from(value: serde_json::Error) -> Self {
        Self::Serialization(value.to_string())
    }
}
