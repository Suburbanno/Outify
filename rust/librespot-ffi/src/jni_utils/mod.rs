pub mod exceptions;
pub mod folders;
pub mod futures;
pub mod logger;
pub mod playback;
                         
pub fn vec_to_jstring_array(env: &mut jni::JNIEnv, vec: Vec<String>) -> jni::sys::jobjectArray {
    let string_class = env.find_class("java/lang/String").unwrap();
    let array = env
        .new_object_array(vec.len() as i32, string_class, jni::objects::JObject::null())
        .unwrap();

    for (i, s) in vec.iter().enumerate() {
        let jstr = env.new_string(s).unwrap();
        env.set_object_array_element(&array, i as i32, jstr).unwrap();
    }

    array.into_raw()
}
