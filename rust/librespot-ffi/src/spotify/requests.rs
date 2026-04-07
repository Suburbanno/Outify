use serde::Serialize;

#[derive(Serialize)]
pub struct AddItemRequest {
    pub uris: Vec<String>,
    pub position: Option<u32>,
}

#[derive(Serialize)]
pub struct RemoveItemRequest {
    pub items: Vec<RemoveItem>,
}

#[derive(Serialize)]
pub struct RemoveItem {
    pub uri: String,
}
