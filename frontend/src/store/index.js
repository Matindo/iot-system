import { createStore } from 'vuex'
import auth from './auth.js'
import project from './project.js'

export default createStore({
  modules: { auth, project },
})
