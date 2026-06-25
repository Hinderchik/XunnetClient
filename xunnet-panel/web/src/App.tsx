import { Box, Typography } from '@mui/material'

function App() {
  return (
    <Box sx={{ p: 4 }}>
      <Typography variant="h4">Xunnet Panel</Typography>
      <Typography variant="body1" sx={{ mt: 2 }}>
        Web management interface for Xunnet federation.
      </Typography>
    </Box>
  )
}

export default App
